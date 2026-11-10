package com.fws.smarttrace.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Plain (non-secret) settings. API keys live in [SecureKeyStore]. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 0 = system, 1 = light, 2 = dark */
    var themeMode: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()

    val nightMode: Int
        get() = when (themeMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    var ttsSpeed: Float
        get() = sp.getFloat("tts_speed", 2f)
        set(v) = sp.edit().putFloat("tts_speed", v).apply()

    var useGemini: Boolean
        get() = sp.getBoolean("use_gemini", false)
        set(v) = sp.edit().putBoolean("use_gemini", v).apply()

    var geminiConsent: Boolean
        get() = sp.getBoolean("gemini_consent", false)
        set(v) = sp.edit().putBoolean("gemini_consent", v).apply()

    var geminiModel: String
        get() = sp.getString("gemini_model", DEFAULT_MODEL)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        set(v) = sp.edit().putString("gemini_model", v.trim()).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var pendingCameraPath: String?
        get() = sp.getString("pending_camera", null)
        set(v) = sp.edit().putString("pending_camera", v).apply()

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    }
}
