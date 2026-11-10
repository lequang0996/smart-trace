package com.fws.smarttrace

import android.content.Context
import android.net.Uri
import com.fws.smarttrace.ai.GeminiClient
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.data.ScanFiles
import com.fws.smarttrace.model.AnalysisMode
import com.fws.smarttrace.model.CodeCheck
import com.fws.smarttrace.model.ExtractedLabel
import com.fws.smarttrace.model.UrlInfo
import com.fws.smarttrace.model.ScanResult
import com.fws.smarttrace.verify.Gtin
import com.fws.smarttrace.verify.RiskEngine
import com.fws.smarttrace.verify.UrlAnalyzer
import com.fws.smarttrace.vision.BarcodeEngine
import com.fws.smarttrace.vision.ImageLabelEngine
import com.fws.smarttrace.vision.ImageUtils
import com.fws.smarttrace.vision.LabelParser
import com.fws.smarttrace.vision.OcrEngine
import java.util.UUID

enum class Stage { PREPARE, CODES, READ, AI, VERIFY, SCORE }

class PipelineException(val resId: Int) : Exception()

/**
 * 1 capture/prepare -> 2 OCR -> 3 field parsing -> 4 visual hints -> 5 (optional) Gemini replaces 2-4
 * -> verify codes -> 6 consistency scoring.
 */
class AnalysisPipeline(private val ctx: Context) {
    private val ocr = OcrEngine()

    suspend fun run(
        uri: Uri,
        useAi: Boolean,
        keys: List<String>,
        onStage: (Stage) -> Unit,
    ): ScanResult {
        onStage(Stage.PREPARE)
        val bmp = ImageUtils.decode(ctx, uri) ?: throw PipelineException(R.string.err_decode)
        val quality = ImageUtils.quality(bmp)

        onStage(Stage.CODES)
        val scanned = BarcodeEngine.scan(bmp)

        var label: ExtractedLabel
        var mode = AnalysisMode.OFFLINE
        var aiFallback: String? = null
        var usedModel = ""
        suspend fun offline(): ExtractedLabel {
            onStage(Stage.READ)
            val read = ocr.read(bmp)
            val hints = ImageLabelEngine.labels(bmp)
            return LabelParser.parse(read, hints)
        }
        if (useAi) {
            onStage(Stage.AI)
            try {
                val out = GeminiClient.extract(ImageUtils.toJpeg(bmp), keys, Languages.promptLanguage())
                label = out.label
                usedModel = out.model
                mode = AnalysisMode.GEMINI
            } catch (e: Exception) {
                aiFallback = (e.message ?: e.javaClass.simpleName).take(80)
                label = offline()
            }
        } else {
            label = offline()
        }

        onStage(Stage.VERIFY)
        val codes = ArrayList<CodeCheck>()
        for (s in scanned) codes += buildCode(s.value, s.format, "scan", s.isUrl)
        if (scanned.isEmpty()) {
            for (t in label.barcodeTexts.map { it.filter(Char::isDigit) }.filter { it.length in intArrayOf(8, 12, 13, 14) }.distinct().take(2)) {
                codes += buildCode(t, "EAN/UPC", "ai", false)
            }
        }

        onStage(Stage.SCORE)
        val risk = RiskEngine.evaluate(label, codes, quality, aiFallback, mode, Languages.isVietnamese())
        val id = UUID.randomUUID().toString()
        runCatching { ScanFiles.save(ctx, id, ImageUtils.scaleDown(bmp, 800)) }
        return ScanResult(
            id = id,
            createdAt = System.currentTimeMillis(),
            mode = mode,
            model = if (mode == AnalysisMode.GEMINI) usedModel else "",
            label = label,
            codes = codes,
            findings = risk.findings,
            verdict = risk.verdict,
            score = risk.score,
        )
    }

    private suspend fun buildCode(value: String, format: String, source: String, isUrl: Boolean): CodeCheck {
        var gtin: String? = null
        var url: UrlInfo? = null
        val kind: String
        if (isUrl) {
            val (info, g) = UrlAnalyzer.analyze(value)
            url = info
            gtin = g
            kind = "URL"
        } else if (value.all { it in '0'..'9' } && value.length in intArrayOf(8, 12, 13, 14)) {
            gtin = value
            kind = "GTIN"
        } else {
            kind = "TEXT"
        }
        var valid: Boolean? = null
        var iso: String? = null
        if (gtin != null) {
            valid = Gtin.isValid(gtin)
            if (valid) {
                iso = Gtin.countryIso(gtin)
            }
        }
        return CodeCheck(value, format, source, kind, gtin, valid, iso, null, url)
    }
}
