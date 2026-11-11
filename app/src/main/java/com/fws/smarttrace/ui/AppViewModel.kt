package com.fws.smarttrace.ui

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fws.smarttrace.AnalysisPipeline
import com.fws.smarttrace.PipelineException
import com.fws.smarttrace.Stage
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.core.Prefs
import com.fws.smarttrace.core.SecureKeyStore
import com.fws.smarttrace.data.HistoryStore
import com.fws.smarttrace.ai.GeminiClient
import com.fws.smarttrace.model.AnalysisMode
import com.fws.smarttrace.model.ScanResult
import com.fws.smarttrace.tts.SpeechController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val stage: Stage) : ScanState
    data class Failed(val resId: Int?, val message: String?) : ScanState
}

data class SettingsState(
    val themeMode: Int,
    val ttsSpeed: Float,
    val useGemini: Boolean,
    val consent: Boolean,
    val onboarded: Boolean,
    val languageChosen: Boolean,
    val keys: List<String>,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val history = HistoryStore(app)
    val speech = SpeechController(app)
    private val pipeline = AnalysisPipeline(app)

    private val _settings = MutableStateFlow(
        SettingsState(
            themeMode = prefs.themeMode, ttsSpeed = prefs.ttsSpeed, useGemini = prefs.useGemini,
            consent = prefs.geminiConsent, 
            onboarded = prefs.onboarded, languageChosen = prefs.languageChosen, keys = emptyList(),
        )
    )
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    private val _opened = MutableStateFlow<ScanResult?>(null)
    val opened: StateFlow<ScanResult?> = _opened.asStateFlow()

    /** Id of the result whose text is being translated into the current app language. */
    private val _translating = MutableStateFlow<String?>(null)
    val translating: StateFlow<String?> = _translating.asStateFlow()
    private val translateTried = HashSet<String>()

    /**
     * A Gemini reading is stored in the language the app had when it was scanned. When the app language is different now,
     * the readable fields are translated once (text only, no photo) and the stored result is updated, so every tab,
     * the history title and the read-aloud text follow the new language.
     */
    fun ensureLanguage(r: ScanResult) {
        val tag = Languages.currentTag()
        if (r.mode != AnalysisMode.GEMINI || r.lang == tag) return
        val s = _settings.value
        if (!s.consent || s.keys.isEmpty() || _translating.value != null) return
        if (!translateTried.add(r.id + "|" + tag)) return
        _translating.value = r.id
        viewModelScope.launch {
            try {
                val label = GeminiClient.translate(r.label, s.keys, Languages.promptLanguage())
                var i = 0
                val findings = r.findings.map { f ->
                    if (f.code == "AI_SIGN") (label.aiSigns.getOrNull(i++)?.let { t -> f.copy(args = listOf(t.take(160))) } ?: f) else f
                }
                val updated = r.copy(label = label, findings = findings, lang = tag)
                history.update(updated)
                if (_opened.value?.id == r.id) _opened.value = updated
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the original text; it is tried again the next time the app starts.
            } finally {
                _translating.value = null
            }
        }
    }

    /** One-shot message shown after saving keys. */
    private val _keysSaved = MutableStateFlow<Int?>(null)
    val keysSaved: StateFlow<Int?> = _keysSaved.asStateFlow()

    init {
        viewModelScope.launch { history.refresh() }
        viewModelScope.launch(Dispatchers.IO) {
            val keys = SecureKeyStore.load(app)
            _settings.update { it.copy(keys = keys) }
        }
    }

    // ---------- scanning ----------
    fun analyze(uri: Uri) {
        if (_scan.value is ScanState.Running) return
        viewModelScope.launch {
            _scan.value = ScanState.Running(Stage.PREPARE)
            try {
                val s = _settings.value
                val useAi = s.useGemini && s.consent && s.keys.isNotEmpty()
                val result = withContext(Dispatchers.Default) {
                    pipeline.run(uri, useAi, s.keys) { st -> _scan.value = ScanState.Running(st) }
                }
                history.insert(result)
                _opened.value = result
                _scan.value = ScanState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: PipelineException) {
                _scan.value = ScanState.Failed(e.resId, null)
            } catch (e: Throwable) {
                _scan.value = ScanState.Failed(null, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun dismissError() { if (_scan.value is ScanState.Failed) _scan.value = ScanState.Idle }

    // ---------- history ----------
    fun open(id: String) {
        viewModelScope.launch { history.get(id)?.let { _opened.value = it } }
    }

    fun closeResult() {
        speech.stop()
        _opened.value = null
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch { history.setFavorite(id, !current) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            if (_opened.value?.id == id) closeResult()
            history.delete(id)
        }
    }

    fun clearHistory() { viewModelScope.launch { history.clearNonFavorites() } }

    // ---------- settings ----------
    fun setTheme(mode: Int) {
        prefs.themeMode = mode
        _settings.update { it.copy(themeMode = mode) }
        AppCompatDelegate.setDefaultNightMode(prefs.nightMode)
    }

    fun setLanguage(tag: String) { Languages.setOverride(tag) }

    fun setSpeed(speed: Float) {
        prefs.ttsSpeed = speed
        _settings.update { it.copy(ttsSpeed = speed) }
        speech.changeSpeed(speed)
    }

    fun setUseGemini(v: Boolean) {
        prefs.useGemini = v
        _settings.update { it.copy(useGemini = v) }
    }

    fun setConsent(v: Boolean) {
        prefs.geminiConsent = v
        _settings.update { it.copy(consent = v) }
    }

    fun finishLanguage() {
        prefs.languageChosen = true
        _settings.update { it.copy(languageChosen = true) }
    }

    fun finishOnboarding() {
        prefs.onboarded = true
        _settings.update { it.copy(onboarded = true) }
    }

    // ---------- API keys ----------
    fun addKeys(text: String) {
        val parsed = SecureKeyStore.parseLines(text)
        if (parsed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val merged = (_settings.value.keys + parsed).distinct()
            SecureKeyStore.save(getApplication<Application>(), merged)
            _settings.update { it.copy(keys = merged) }
            _keysSaved.value = parsed.size
        }
    }

    fun removeKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rest = _settings.value.keys - key
            SecureKeyStore.save(getApplication<Application>(), rest)
            _settings.update { it.copy(keys = rest, useGemini = if (rest.isEmpty()) false else it.useGemini) }
            if (rest.isEmpty()) prefs.useGemini = false
        }
    }

    fun keysSavedShown() { _keysSaved.value = null }

    override fun onCleared() {
        speech.shutdown()
        super.onCleared()
    }
}
