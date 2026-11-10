package com.fws.smarttrace.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object Languages {
    /** Language tags supported by the app, in display order. */
    val supported = listOf("en", "zh-CN", "es", "pt", "ru", "ja", "ko", "vi")

    /** Native names (never translated, so people can always find their language). */
    val nativeNames = mapOf(
        "en" to "English",
        "zh-CN" to "简体中文",
        "es" to "Español",
        "pt" to "Português",
        "ru" to "Русский",
        "ja" to "日本語",
        "ko" to "한국어",
        "vi" to "Tiếng Việt",
    )

    /** The language tag in use. English is the default; the app never follows the system language. */
    fun currentTag(): String {
        val l = AppCompatDelegate.getApplicationLocales()
        if (l.isEmpty) return "en"
        val tag = l.toLanguageTags().substringBefore(',')
        return supported.firstOrNull { it.equals(tag, true) }
            ?: supported.firstOrNull { it.substringBefore('-') == tag.substringBefore('-') }
            ?: "en"
    }

    /** First launch (or after an upgrade from "system default"): pin the UI language to English. */
    fun ensureDefault() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) setOverride("en")
    }

    fun setOverride(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    /** The locale the UI is actually shown in. */
    fun effectiveLocale(): Locale {
        val o = AppCompatDelegate.getApplicationLocales()
        if (!o.isEmpty) o.get(0)?.let { return it }
        return Locale.ENGLISH
    }

    fun isVietnamese(): Boolean = effectiveLocale().language == "vi"

    /** English language name, used inside the Gemini prompt. */
    fun promptLanguage(): String {
        val l = effectiveLocale()
        if (l.language == "zh") return "Simplified Chinese"
        return l.getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }
    }
}
