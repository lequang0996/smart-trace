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

    /** "" means "follow the system". */
    fun currentTag(): String {
        val l = AppCompatDelegate.getApplicationLocales()
        return if (l.isEmpty) "" else l.toLanguageTags().substringBefore(',')
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
        val sys = Locale.getDefault()
        return if (supported.any { it.substringBefore('-') == sys.language }) sys else Locale.ENGLISH
    }

    fun isVietnamese(): Boolean = effectiveLocale().language == "vi"

    /** English language name, used inside the Gemini prompt. */
    fun promptLanguage(): String = effectiveLocale().getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }
}
