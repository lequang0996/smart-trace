package com.fws.smarttrace.verify

import com.fws.smarttrace.core.DateUtil
import com.fws.smarttrace.core.TextUtil
import com.fws.smarttrace.core.Units
import com.fws.smarttrace.model.AnalysisMode
import com.fws.smarttrace.model.CodeCheck
import com.fws.smarttrace.model.ExtractedLabel
import com.fws.smarttrace.model.Finding
import com.fws.smarttrace.model.Severity
import com.fws.smarttrace.model.Verdict
import com.fws.smarttrace.vision.ImageUtils
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rule-based consistency check. It never claims a product is genuine: it only reports whether the
 * label agrees with itself and with whatever public data could be checked.
 */
object RiskEngine {
    data class Output(val findings: List<Finding>, val verdict: Verdict, val score: Int)

    private val SUSPICIOUS_BAD = setOf("GTIN_BAD", "AREA_DIFF")
    private val QTY_RE = Regex("""(\d+[.,]?\d*)\s*(kg|mg|g|ml|cl|l|oz)\b""", RegexOption.IGNORE_CASE)

    private fun fmt(d: Double): String = String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

    /** Quantity text -> base unit (grams or millilitres). */
    private fun toBase(num: String, unit: String): Double? {
        val n = TextUtil.parseNum(num) ?: return null
        return when (unit.lowercase()) {
            "kg" -> n * 1000; "g" -> n; "mg" -> n / 1000
            "l" -> n * 1000; "ml" -> n; "cl" -> n * 10
            "oz" -> n * 28.35
            else -> null
        }
    }

    fun evaluate(
        label: ExtractedLabel,
        codes: List<CodeCheck>,
        quality: ImageUtils.Quality,
        aiFallback: String?,
        mode: AnalysisMode,
        vietnamRules: Boolean,
        today: LocalDate = LocalDate.now(),
    ): Output {
        val f = ArrayList<Finding>()
        fun add(code: String, sev: Severity, vararg args: String) { f += Finding(code, sev, args.toList()) }

        val fieldCount = listOf(
            label.productName, label.brand, label.manufacturer, label.size, label.batch,
            label.shade, label.mfgDate, label.expDate, label.grade, label.itemCode,
        ).count { it.isNotBlank() }
        val textLen = label.rawText.length
        val tooLittle = textLen < 15 && fieldCount < 2
        val insufficient = tooLittle && codes.isEmpty()

        if (aiFallback != null) add("AI_FALLBACK", Severity.INFO, aiFallback)
        if (quality.blurry) add("BLUR", Severity.WARN)
        if (quality.dark) add("DARK", Severity.WARN)
        if (tooLittle) add("LOWTEXT", Severity.WARN)

        if (!tooLittle) {
            // --- arithmetic on the printed numbers ---
            val size = Units.sizeMm(label.size)
            val qty = label.quantityPcs
            val area = label.areaSqm
            if (size != null && qty != null && area != null && area > 0) {
                val calc = qty * size.first * size.second / 1_000_000.0
                val diff = abs(calc - area) / area
                val sizeText = "${size.first.roundToInt()}×${size.second.roundToInt()} mm"
                when {
                    diff <= 0.03 -> add("AREA_OK", Severity.OK, qty.toString(), sizeText, fmt(calc))
                    diff <= 0.10 -> add("AREA_DIFF", Severity.WARN, qty.toString(), sizeText, fmt(calc), fmt(area))
                    else -> add("AREA_DIFF", Severity.BAD, qty.toString(), sizeText, fmt(calc), fmt(area))
                }
            }

            // --- who made it ---
            val maker = label.manufacturer.ifBlank { label.brand }
            if (maker.isBlank()) add("NO_MAKER", Severity.WARN)
            else {
                add("MAKER_OK", Severity.OK, maker.take(60))
                if (label.phone.isBlank() && label.address.isBlank()) add("CONTACT_MISSING", Severity.INFO)
                else add("CONTACT_OK", Severity.OK)
            }

            // --- traceability and dates ---
            val trace = listOf(label.batch, label.shade, label.mfgDate, label.expDate).filter { it.isNotBlank() }
            if (trace.isEmpty() && label.shelfLifeMonths == null) add("TRACE_MISSING", Severity.WARN)
            else if (trace.isNotEmpty()) add("TRACE_OK", Severity.OK, trace.joinToString(" · "))

            var exp = DateUtil.parse(label.expDate, endOfMonth = true)
            val mfg = DateUtil.parse(label.mfgDate)
            val months = label.shelfLifeMonths
            if (months != null) {
                add("SHELF", Severity.INFO, months.toString())
                if (exp == null && mfg != null) exp = mfg.plusMonths(months.toLong())
            }
            exp?.let {
                when {
                    it.isBefore(today) -> add("EXP_PAST", Severity.BAD, it.toString())
                    it.isBefore(today.plusDays(60)) -> add("EXP_SOON", Severity.WARN, it.toString())
                    else -> add("EXP_OK", Severity.OK, it.toString())
                }
            }

            // --- legal / certificates ---
            if (vietnamRules) {
                when (label.hasVietnameseLabel) {
                    true -> add("VN_OK", Severity.OK)
                    false -> add("VN_MISSING", Severity.WARN)
                    null -> {}
                }
            }
            if (label.standards.isNotEmpty()) add("STD", Severity.INFO, label.standards.joinToString(", "))
            else add("STD_NONE", Severity.INFO)
            if (label.grade.isNotBlank()) add("GRADE", Severity.INFO, label.grade)

            // --- things the AI noticed ---
            if (label.handwritten.isNotEmpty()) add("HAND", Severity.WARN, label.handwritten.joinToString(", "))
            label.aiSigns.take(3).forEach { add("AI_SIGN", Severity.WARN, it.take(160)) }
            val conf = label.confidence
            if (mode == AnalysisMode.GEMINI && conf != null && conf < 0.5) add("AI_LOWCONF", Severity.INFO)
        }

        // --- barcodes and QR codes ---
        if (codes.isEmpty()) add("NO_CODE", Severity.INFO)
        for (c in codes) {
            if (c.source == "ai") add("AI_BARCODE", Severity.INFO)
            val shown = c.gtin ?: c.value
            c.url?.let { u ->
                add("URL", Severity.INFO, u.host.ifBlank { u.url.take(60) })
                if ("http" in u.flags) add("URL_HTTP", Severity.WARN)
                if ("short" in u.flags) add("URL_SHORT", Severity.WARN)
                if ("ip" in u.flags) add("URL_IP", Severity.WARN)
                if ("puny" in u.flags) add("URL_PUNY", Severity.WARN)
                if (c.gtin != null) add("GS1LINK", Severity.INFO, c.gtin)
            }
            if (c.kind == "TEXT") add("QR_TEXT", Severity.INFO, c.value.take(80))

            val valid = c.gtinValid
            if (valid != null) {
                if (!valid) add("GTIN_BAD", Severity.BAD, shown)
                else {
                    add("GTIN_OK", Severity.OK, shown)
                    add("GS1_VERIFY", Severity.INFO, shown)
                    val iso = c.prefixIso
                    if (iso != null) {
                        val originIso = label.originIso.ifBlank { null } ?: Gtin.originToIso(label.origin)
                        if (originIso != null && originIso != iso) add("PREFIX_DIFF", Severity.INFO, label.origin, "iso:$iso")
                        else add("PREFIX", Severity.INFO, Gtin.prefix3(shown) ?: "", "iso:$iso")
                    }
                }
            }
        }

        val oks = f.count { it.severity == Severity.OK }
        val warns = f.count { it.severity == Severity.WARN }
        val bads = f.count { it.severity == Severity.BAD }
        var score = 60 + minOf(oks * 5, 30) - warns * 8 - bads * 20
        score = score.coerceIn(0, 100)

        val suspicious = f.any { it.severity == Severity.BAD && it.code in SUSPICIOUS_BAD } ||
            (f.any { it.code == "DB_BRAND_MISS" } && f.any { it.code == "DB_NAME_DIFF" })
        val verdict = when {
            insufficient -> Verdict.INSUFFICIENT
            suspicious -> Verdict.SUSPICIOUS
            warns == 0 && bads == 0 && oks >= 3 -> Verdict.CONSISTENT
            else -> Verdict.CHECK
        }
        if (insufficient) score = minOf(score, 30)
        return Output(f, verdict, score)
    }
}
