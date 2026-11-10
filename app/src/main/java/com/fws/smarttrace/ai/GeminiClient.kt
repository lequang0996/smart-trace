package com.fws.smarttrace.ai

import android.util.Base64
import com.fws.smarttrace.model.ExtractedLabel
import com.fws.smarttrace.model.bool
import com.fws.smarttrace.model.dbl
import com.fws.smarttrace.model.int
import com.fws.smarttrace.model.str
import com.fws.smarttrace.model.strings
import com.fws.smarttrace.vision.LabelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiException(message: String) : Exception(message)

/**
 * Minimal Gemini REST client. The user's own API keys are tried in order; a key that hits a limit
 * is parked for a while and the next one is used. Keys are sent in a header and never logged.
 */
object GeminiClient {
    private val cooldownUntil = HashMap<String, Long>()

    private const val PROMPT = """You are a packaging-label reader for a product traceability app. Read the photo (carton, bag, label, barcode).
Return ONLY one JSON object, with no markdown. Use "" or null when a value is not visible. NEVER guess or invent values.
Schema:
{"productName":"","brand":"","category":"tile|adhesive|paint|cement|food|cosmetic|electronics|other","manufacturer":"","origin":"","address":"","phone":"","size":"","quantityPcs":null,"areaSqm":null,"netWeightKg":null,"grossWeightKg":null,"grade":"","itemCode":"","batch":"","shade":"","mfgDate":"","expDate":"","shelfLifeMonths":null,"standards":[],"specs":[{"label":"","value":""}],"barcodes":[],"hasVietnameseLabel":false,"handwrittenFields":[],"suspiciousSigns":[],"rawText":"","confidence":0.0}
Rules:
- Transcribe text exactly as printed; the photo may be sideways or upside down. Keep the original language for names and dates.
- "handwrittenFields": names of fields whose value is handwritten or overwritten by pen.
- "barcodes": digits printed under any barcode, or QR text you can read.
- "specs": other technical specifications printed on the pack, as label/value pairs in the printed language.
- "hasVietnameseLabel": true only if Vietnamese-language label text is present.
- "suspiciousSigns": up to 3 clearly visible packaging problems (misspelled brand, mismatched numbers, inconsistent print), written in {LANG}; empty list if none.
- "confidence": 0 to 1 for the overall reading. "rawText": all legible text, at most 1500 characters."""

    data class Outcome(val label: ExtractedLabel)

    suspend fun extract(jpeg: ByteArray, keys: List<String>, model: String, language: String): Outcome =
        withContext(Dispatchers.IO) {
            if (keys.isEmpty()) throw GeminiException("no key")
            val safeModel = model.filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
            if (safeModel.isEmpty()) throw GeminiException("bad model")
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray()
                        .put(JSONObject().put("text", PROMPT.replace("{LANG}", language)))
                        .put(JSONObject().put("inline_data", JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP)))))))
                .put("generationConfig", JSONObject().put("temperature", 0.1).put("responseMimeType", "application/json"))
                .toString()

            val now = System.currentTimeMillis()
            val (ready, cooling) = keys.partition { (cooldownUntil[it] ?: 0L) <= now }
            var last = "unknown error"
            for (key in ready + cooling) {
                val (code, text) = try {
                    post(safeModel, key, body)
                } catch (e: Exception) {
                    last = e.javaClass.simpleName
                    cooldownUntil[key] = System.currentTimeMillis() + 10_000
                    continue
                }
                if (code in 200..299) {
                    return@withContext Outcome(parse(text))
                }
                last = "HTTP $code ${errorMessage(text)}".trim()
                when (code) {
                    429 -> cooldownUntil[key] = System.currentTimeMillis() + 60_000
                    401, 403 -> cooldownUntil[key] = System.currentTimeMillis() + 600_000
                    400 -> if (text.contains("API key", true) || text.contains("API_KEY", true)) {
                        cooldownUntil[key] = System.currentTimeMillis() + 600_000
                    } else throw GeminiException(last)
                    404 -> throw GeminiException(last)
                    else -> cooldownUntil[key] = System.currentTimeMillis() + 10_000
                }
            }
            throw GeminiException(last)
        }

    private fun post(model: String, key: String, body: String): Pair<Int, String> {
        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 70_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(text: String): String = try {
        JSONObject(text).optJSONObject("error")?.optString("message", "")?.take(120) ?: ""
    } catch (e: Exception) { "" }

    private fun parse(responseText: String): ExtractedLabel {
        val root = try { JSONObject(responseText) } catch (e: Exception) { throw GeminiException("bad response") }
        val cands = root.optJSONArray("candidates")
        if (cands == null || cands.length() == 0) {
            val reason = root.optJSONObject("promptFeedback")?.optString("blockReason", "") ?: ""
            throw GeminiException(if (reason.isNotBlank()) "blocked: $reason" else "empty response")
        }
        val parts = cands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
        val sb = StringBuilder()
        if (parts != null) for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (p.optBoolean("thought", false)) continue
            sb.append(p.optString("text", ""))
        }
        var t = sb.toString().trim()
        if (t.startsWith("```")) t = t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        if (start < 0 || end <= start) throw GeminiException("no JSON in response")
        val o = try { JSONObject(t.substring(start, end + 1)) } catch (e: Exception) { throw GeminiException("bad JSON") }
        return toLabel(o)
    }

    private fun toLabel(o: JSONObject): ExtractedLabel {
        val specsArr = o.optJSONArray("specs")
        val specs = if (specsArr == null) emptyList() else List(specsArr.length()) { i ->
            val s = specsArr.optJSONObject(i)
            (s?.str("label") ?: "") to (s?.str("value") ?: "")
        }.filter { it.first.isNotBlank() && it.second.isNotBlank() }.take(20)
        val raw = o.str("rawText")
        val cat = o.str("category").lowercase().let {
            if (it in setOf("tile", "adhesive", "paint", "cement", "food", "cosmetic", "electronics", "other")) it
            else LabelParser.category(raw + " " + o.str("productName"), emptyList())
        }
        return ExtractedLabel(
            productName = o.str("productName"), brand = o.str("brand"), category = cat,
            manufacturer = o.str("manufacturer"), origin = o.str("origin"), address = o.str("address"), phone = o.str("phone"),
            size = o.str("size"), quantityPcs = o.int("quantityPcs"), areaSqm = o.dbl("areaSqm"),
            netWeightKg = o.dbl("netWeightKg"), grossWeightKg = o.dbl("grossWeightKg"),
            grade = o.str("grade"), itemCode = o.str("itemCode"), batch = o.str("batch"), shade = o.str("shade"),
            mfgDate = o.str("mfgDate"), expDate = o.str("expDate"), shelfLifeMonths = o.int("shelfLifeMonths"),
            standards = o.optJSONArray("standards").strings().take(6), extraSpecs = specs,
            hasVietnameseLabel = o.bool("hasVietnameseLabel"),
            handwritten = o.optJSONArray("handwrittenFields").strings().take(6),
            rawText = raw.take(4000), aiSigns = o.optJSONArray("suspiciousSigns").strings().take(3),
            confidence = o.dbl("confidence"), barcodeTexts = o.optJSONArray("barcodes").strings().take(4),
        )
    }
}
