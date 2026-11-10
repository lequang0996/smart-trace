package com.fws.smarttrace.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SpeechState(
    val playing: Boolean = false,
    val index: Int = -1,
    val missingVoice: Boolean = false,
)

/** Text-to-speech with a speed of up to 4x. Paragraphs are queued so the one being read can be highlighted. */
class SpeechController(context: Context) {
    private val app = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: (() -> Unit)? = null

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var paragraphs: List<String> = emptyList()
    private var locale: Locale = Locale.getDefault()
    private var rate: Float = 2f

    private fun ensure(onReady: () -> Unit) {
        if (ready) { onReady(); return }
        pending = onReady
        if (tts != null) return
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val i = utteranceId?.toIntOrNull() ?: return
                        _state.value = _state.value.copy(playing = true, index = i)
                    }
                    override fun onDone(utteranceId: String?) {
                        val i = utteranceId?.toIntOrNull() ?: return
                        if (i >= paragraphs.size - 1) _state.value = _state.value.copy(playing = false, index = -1)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(playing = false, index = -1)
                    }
                })
                pending?.invoke()
                pending = null
            } else {
                _state.value = SpeechState(missingVoice = true)
            }
        }
    }

    fun speak(list: List<String>, loc: Locale, speed: Float, from: Int = 0) {
        paragraphs = list.filter { it.isNotBlank() }
        locale = loc
        rate = speed
        if (paragraphs.isEmpty()) return
        ensure {
            val engine = tts ?: return@ensure
            val r = engine.setLanguage(locale)
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                _state.value = SpeechState(missingVoice = true)
                return@ensure
            }
            engine.setSpeechRate(rate.coerceIn(0.5f, 4f))
            _state.value = SpeechState(playing = true, index = from)
            for (i in from until paragraphs.size) {
                engine.speak(paragraphs[i], if (i == from) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, i.toString())
            }
        }
    }

    /** Applies a new speed; if something is being read, continues from the current paragraph at the new speed. */
    fun changeSpeed(speed: Float) {
        rate = speed
        val s = _state.value
        if (s.playing && s.index >= 0) speak(paragraphs, locale, speed, s.index)
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(playing = false, index = -1)
    }

    fun clearMissingFlag() {
        _state.value = _state.value.copy(missingVoice = false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
