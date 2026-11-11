package com.fws.smarttrace.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.fws.smarttrace.core.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.min

data class OcrLine(val text: String, val height: Int)
/** [quality]: share of the recognised characters that look like real words (0..1). */
data class OcrResult(val text: String, val rotation: Int, val lines: List<OcrLine>, val quality: Double = 1.0)

/** On-device text recognition (Latin + Chinese). Tries 4 orientations because label photos are often sideways. */
class OcrEngine {
    private val latin: TextRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val chinese: TextRecognizer by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }

    private suspend fun run(rec: TextRecognizer, bmp: Bitmap, rotation: Int): OcrResult {
        val t = rec.process(InputImage.fromBitmap(bmp, rotation)).await()
        val lines = t.textBlocks.flatMap { b ->
            b.lines.map { l ->
                val box = l.boundingBox
                OcrLine(l.text, if (box == null) 0 else min(box.width(), box.height()))
            }
        }
        return OcrResult(t.text, rotation, lines)
    }

    /** Grey-scale + stronger contrast: helps on glossy, low-contrast or slightly dark packaging. */
    private fun enhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = 1.7f
        val t = (-0.5f * c + 0.5f) * 255f
        val cm = ColorMatrix().apply { setSaturation(0f) }
        cm.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        return out
    }

    /** Sum of the length of the lines that look like real text, plus a bonus for label keywords. */
    private fun merit(r: OcrResult): Int =
        r.lines.filter { !isJunk(it.text) }.sumOf { it.text.length } + score(r.text) * 5

    suspend fun read(bmp: Bitmap): OcrResult {
        var best = run(latin, bmp, 0)
        var bestScore = score(best.text)
        if (bestScore < 25) {
            for (rot in intArrayOf(90, 270, 180)) {
                val r = runCatching { run(latin, bmp, rot) }.getOrNull() ?: continue
                val s = score(r.text)
                if (s > bestScore) { bestScore = s; best = r }
            }
        }
        // Second opinion on a contrast-enhanced copy; keep whichever pass reads more real text.
        val enhanced = runCatching { enhance(bmp) }.getOrNull()
        if (enhanced != null) {
            val r = runCatching { run(latin, enhanced, best.rotation) }.getOrNull()
            if (r != null && merit(r) > merit(best)) best = r
            enhanced.recycle()
        }
        val zh = runCatching { run(chinese, bmp, best.rotation) }.getOrNull()
        if (zh != null && (HAN.containsMatchIn(zh.text) || zh.text.length > best.text.length * 1.2)) {
            best = zh
        }
        return withoutJunk(best)
    }

    /** Drops garbled lines (they only confuse the parser and the reader) and reports how much text was usable. */
    private fun withoutJunk(r: OcrResult): OcrResult {
        val total = r.lines.sumOf { it.text.length }.coerceAtLeast(1)
        val keep = r.lines.filter { !isJunk(it.text) }
        val q = keep.sumOf { it.text.length }.toDouble() / total
        return r.copy(text = keep.joinToString("\n") { it.text }, lines = keep, quality = q)
    }

    fun close() {
        runCatching { latin.close() }
        runCatching { chinese.close() }
    }

    companion object {
        private val HAN = Regex("\\p{IsHan}")
        private val WORD = Regex("[A-Za-z]{4,}")
        private val KEYWORDS = listOf(
            "GRADE", "SIZE", "QTY", "ITEM", "WEIGHT", "KG", "PCS", "SQM", "MM", "TILE", "DATE", "EXP", "MFG",
            "MADE", "BATCH", "LOT", "NET", "GROSS", "COLOR", "COLOUR", "SHADE", "PRODUCT", "ADHESIVE", "CEMENT", "STANDARD",
        )

        private val VOWEL = Regex("[aeiouyàáâãäåæèéêëìíîïòóôõöøùúûüýăâêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]")
        private val DIGIT_IN_WORD = Regex("[a-zà-ỹ]\\d[a-zà-ỹ]")

        /** A single recognised word that is almost certainly an OCR error (no vowel, or a digit in the middle of a word). */
        private fun junkToken(tok: String): Boolean {
            val w = tok.trim('.', ',', ':', ';', '(', ')', '-', '/', '"', '\'')
            if (w.length < 3) return false
            if (w.all { it.isUpperCase() || !it.isLetter() }) return false          // CAPS, codes, numbers
            val lower = w.lowercase()
            if (lower.any { !it.isLetterOrDigit() && it != '-' }) return w.count { !it.isLetterOrDigit() } >= 2
            if (DIGIT_IN_WORD.containsMatchIn(lower)) return true
            return lower.all { it in 'a'..'z' } && w.length >= 3 && !VOWEL.containsMatchIn(lower)
        }

        /** True for lines that are mostly symbols or made of broken words. Lines with long numbers are always kept. */
        fun isJunk(line: String): Boolean {
            val t = line.trim()
            if (t.length < 2) return true
            if (t.count { it.isDigit() } >= 6) return false
            if (HAN.findAll(t).count() >= 2) return false
            val compact = t.filterNot { it.isWhitespace() }
            val alnum = compact.count { it.isLetterOrDigit() }
            if (alnum.toDouble() / compact.length < 0.6) return true
            val tokens = t.split(Regex("\\s+")).filter { it.any(Char::isLetter) }
            if (tokens.size < 2) return tokens.isNotEmpty() && junkToken(tokens[0]) && t.length < 8
            val bad = tokens.count { junkToken(it) }
            return bad * 3 >= tokens.size
        }

        /** Higher = looks more like real label text (used to pick the right orientation). */
        fun score(text: String): Int {
            val up = text.uppercase()
            return KEYWORDS.count { up.contains(it) } * 20 + WORD.findAll(text).count() + HAN.findAll(text).count() / 2
        }
    }
}
