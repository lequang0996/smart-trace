package com.fws.smarttrace.vision

import android.graphics.Bitmap
import com.fws.smarttrace.core.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.min

data class OcrLine(val text: String, val height: Int)
data class OcrResult(val text: String, val rotation: Int, val lines: List<OcrLine>)

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
        val zh = runCatching { run(chinese, bmp, best.rotation) }.getOrNull()
        if (zh != null && (HAN.containsMatchIn(zh.text) || zh.text.length > best.text.length * 1.2)) {
            best = zh
        }
        return best
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

        /** Higher = looks more like real label text (used to pick the right orientation). */
        fun score(text: String): Int {
            val up = text.uppercase()
            return KEYWORDS.count { up.contains(it) } * 20 + WORD.findAll(text).count() + HAN.findAll(text).count() / 2
        }
    }
}
