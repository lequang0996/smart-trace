package com.fws.smarttrace.core

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Suspends until a Play-services [Task] completes. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

object TextUtil {
    private val MARKS = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val SPLIT = Regex("[^a-z0-9\\p{IsHan}]+")

    /** Lower-case, accent-free text for fuzzy comparison. */
    fun fold(s: String): String =
        MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
            .replace('đ', 'd').replace('Đ', 'D').lowercase()

    fun tokens(s: String, minLen: Int = 3): Set<String> =
        fold(s).split(SPLIT).filter { it.length >= minLen }.toSet()

    fun parseNum(s: String): Double? {
        var t = s.trim().replace(" ", "")
        if (t.isEmpty()) return null
        t = if (t.contains(',') && t.contains('.')) {
            if (t.lastIndexOf(',') > t.lastIndexOf('.')) t.replace(".", "").replace(',', '.') else t.replace(",", "")
        } else t.replace(',', '.')
        return t.toDoubleOrNull()
    }
}

object Units {
    private val SIZE = Regex("""(\d{2,4})\s*[xX×*]\s*(\d{2,4})\s*(mm|cm)?""", RegexOption.IGNORE_CASE)

    /** Returns (width, height) in millimetres. Values <= 120 without a unit are read as centimetres. */
    fun sizeMm(text: String): Pair<Double, Double>? {
        val m = SIZE.find(text) ?: return null
        var w = m.groupValues[1].toDouble()
        var h = m.groupValues[2].toDouble()
        val unit = m.groupValues[3].lowercase()
        val cm = unit == "cm" || (unit.isEmpty() && maxOf(w, h) <= 120)
        if (cm) { w *= 10; h *= 10 }
        return w to h
    }
}

object DateUtil {
    private val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    private fun year(s: String): Int = s.toInt().let { if (s.length <= 2) 2000 + it else it }

    private fun ymd(y: Int, m: Int, d: Int?, endOfMonth: Boolean): LocalDate? {
        if (m !in 1..12 || y !in 2000..2100) return null
        return try {
            when {
                d != null -> LocalDate.of(y, m, d)
                endOfMonth -> YearMonth.of(y, m).atEndOfMonth()
                else -> LocalDate.of(y, m, 1)
            }
        } catch (e: Exception) { null }
    }

    /** Understands dd/mm/yyyy, yyyy-mm-dd, mm/yyyy, 12 MAR 2026, MAR 2026, 2026年3月. Day-first by default. */
    fun parse(raw: String, endOfMonth: Boolean = false): LocalDate? {
        val s = raw.trim().uppercase()
        if (s.isEmpty()) return null
        Regex("""^(\d{4})\s*[./\-年]\s*(\d{1,2})(?:\s*[./\-月]\s*(\d{1,2}))?""").find(s)?.let {
            return ymd(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toIntOrNull(), endOfMonth)
        }
        Regex("""^(\d{1,2})\s*[./\-]\s*(\d{1,2})\s*[./\-]\s*(\d{2,4})""").find(s)?.let {
            var a = it.groupValues[1].toInt()
            var b = it.groupValues[2].toInt()
            if (b > 12 && a <= 12) { val t = a; a = b; b = t }
            return ymd(year(it.groupValues[3]), b, a, endOfMonth)
        }
        Regex("""^(\d{1,2})\s*[./\-]\s*(\d{4})$""").find(s)?.let {
            return ymd(it.groupValues[2].toInt(), it.groupValues[1].toInt(), null, endOfMonth)
        }
        Regex("""^(\d{1,2})\s?([A-Z]{3})[A-Z]*\.?\s?(\d{2,4})""").find(s)?.let {
            val mi = MONTHS.indexOf(it.groupValues[2]) + 1
            return ymd(year(it.groupValues[3]), mi, it.groupValues[1].toInt(), endOfMonth)
        }
        Regex("""^([A-Z]{3})[A-Z]*\.?[ ,\-]*(\d{2,4})$""").find(s)?.let {
            val mi = MONTHS.indexOf(it.groupValues[1]) + 1
            return ymd(year(it.groupValues[2]), mi, null, endOfMonth)
        }
        return null
    }
}
