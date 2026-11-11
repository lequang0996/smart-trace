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
import kotlinx.coroutines.delay
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

    private const val PROMPT = """You are a packaging-label reader and translator for a product traceability app. Read the photo (carton, bag, label, barcode).
Return ONLY one JSON object, with no markdown. Use "" or null when a value is not visible. NEVER guess or invent values.
Schema:
{"productName":"","brand":"","category":"tile|adhesive|paint|cement|food|cosmetic|electronics|other","manufacturer":"","origin":"","originIso":"","address":"","phone":"","size":"","quantityPcs":null,"areaSqm":null,"netWeightKg":null,"grossWeightKg":null,"grade":"","itemCode":"","batch":"","shade":"","mfgDate":"","expDate":"","shelfLifeMonths":null,"standards":[],"specs":[{"label":"","value":""}],"barcodes":[],"hasVietnameseLabel":false,"handwrittenFields":[],"suspiciousSigns":[],"marks":[{"mark":"","meaning":""}],"marketNote":"","rawText":"","confidence":0.0}
Reading rules:
- The photo may be sideways or upside down. Read it carefully and transcribe exactly what is printed.
- "handwrittenFields": names of fields whose value is handwritten or overwritten by pen.
- "barcodes": digits printed under any barcode, or QR text you can read.
- "specs": every other technical specification printed on the pack (ingredients, usage, directions, warnings, storage, model number, memory / storage size, colour, package contents, serial number, IMEI, EID, etc.) as label/value pairs.
- "marks": every certification, compliance, safety, recycling or quality mark or symbol that is printed or drawn on the pack (for example CE, UKCA, FCC, RoHS, the crossed-out wheelie bin WEEE, Triman, Bac de tri, recycling codes such as PAP 21, FSC, CCC, BIS, QC PASS, Hi-Res Audio). "mark" is its name as shown; "meaning" is one short explanation in {LANG}. List only marks that are really visible in the photo; never add a mark because the product type usually has it.
- "marketNote": one short sentence in {LANG} about which market or region the pack appears to be made for, ONLY when there is printed evidence (a printed list of country codes, text blocks in regional languages, regional certification marks). Otherwise "".
- "hasVietnameseLabel": true only if Vietnamese-language label text is present.
- "originIso": ISO 3166-1 alpha-2 code of the country of origin when it is stated or clearly implied (for example CN, VN, JP), otherwise "".
- "confidence": 0 to 1 for the overall reading.
- "rawText": all legible text EXACTLY as printed, in its ORIGINAL language and script (never translate it), at most 1500 characters.
Translation rules (the app user reads {LANG}):
- Translate into {LANG}: "productName", "manufacturer", "origin", "address", "grade", "standards" descriptions, every "specs" label and every "specs" value, and "suspiciousSigns". If the printed text is already in {LANG}, keep it as is.
- "brand": keep the trademark exactly as printed. If it is written in a script other than the one {LANG} uses, append a short transliteration or translation in parentheses, e.g. "BrandName (translation)".
- Names of companies and places: translate or transliterate them the way {LANG} speakers normally write them (for Vietnamese use Han-Viet for Chinese names), keep codes and street numbers.
- Do NOT translate or alter numbers, units, dates, phone numbers, batch / lot / item / shade codes, standard codes (e.g. GB 15979-2002), barcodes, or "category". Keep dates in the printed format.
- If something is unreadable, leave it empty instead of guessing.
- "suspiciousSigns": up to 3 clearly visible packaging problems (misspelled brand, mismatched numbers, inconsistent print), written in {LANG}; empty list if none."""

    data class Outcome(val label: ExtractedLabel, val model: String)

    /** Tried in this order: the default first, the second one only when the first cannot be used. */
    val MODELS = listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite")
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 5_000L

    private sealed interface Attempt {
        class Ok(val label: ExtractedLabel) : Attempt
        /** [switchModel]: retrying the same model is pointless (quota used up, model unavailable, bad request). */
        class Failed(val message: String, val switchModel: Boolean) : Attempt
    }

    /**
     * Up to [MAX_ATTEMPTS] tries per model, [RETRY_DELAY_MS] apart. When the quota of a model is used up on every key
     * the next model is used straight away. When both models fail this throws, and the caller falls back to offline.
     */
    suspend fun extract(jpeg: ByteArray, keys: List<String>, language: String): Outcome =
        withContext(Dispatchers.IO) {
            if (keys.isEmpty()) throw GeminiException("no key")
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

            var last = "unknown error"
            for ((index, model) in MODELS.withIndex()) {
                val lastModel = index == MODELS.lastIndex
                for (attempt in 1..MAX_ATTEMPTS) {
                    when (val r = tryModel(model, keys, body, includeCooling = lastModel)) {
                        is Attempt.Ok -> return@withContext Outcome(r.label, model)
                        is Attempt.Failed -> {
                            last = r.message
                            if (r.switchModel) break
                            if (attempt < MAX_ATTEMPTS) delay(RETRY_DELAY_MS)
                        }
                    }
                }
            }
            throw GeminiException(last)
        }

    /** One pass over the keys for one model. */
    private fun tryModel(model: String, keys: List<String>, body: String, includeCooling: Boolean): Attempt {
        val now = System.currentTimeMillis()
        val (ready, cooling) = keys.partition { (cooldownUntil["$model|$it"] ?: 0L) <= now }
        val candidates = if (includeCooling) ready + cooling else ready
        if (candidates.isEmpty()) return Attempt.Failed("HTTP 429 quota", switchModel = true)

        var last = "unknown error"
        var quotaOnly = true
        for (key in candidates) {
            val id = "$model|$key"
            val (code, text) = try {
                post(model, key, body)
            } catch (e: Exception) {
                last = e.javaClass.simpleName
                quotaOnly = false
                cooldownUntil[id] = System.currentTimeMillis() + 10_000
                continue
            }
            if (code in 200..299) {
                return try { Attempt.Ok(parse(text)) } catch (e: GeminiException) {
                    Attempt.Failed(e.message ?: "bad response", switchModel = false)
                }
            }
            last = "HTTP $code ${errorMessage(text)}".trim()
            if (code != 429) quotaOnly = false
            when (code) {
                429 -> cooldownUntil[id] = System.currentTimeMillis() + 60_000
                401, 403 -> cooldownUntil[id] = System.currentTimeMillis() + 600_000
                400 -> if (text.contains("API key", true) || text.contains("API_KEY", true)) {
                    cooldownUntil[id] = System.currentTimeMillis() + 600_000
                } else return Attempt.Failed(last, switchModel = true)
                404 -> return Attempt.Failed(last, switchModel = true)
                else -> cooldownUntil[id] = System.currentTimeMillis() + 10_000
            }
        }
        return Attempt.Failed(last, switchModel = quotaOnly)
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
            manufacturer = o.str("manufacturer"), origin = o.str("origin"), originIso = o.str("originIso").uppercase().takeIf { it.length == 2 && it.all(Char::isLetter) } ?: "", address = o.str("address"), phone = o.str("phone"),
            size = o.str("size"), quantityPcs = o.int("quantityPcs"), areaSqm = o.dbl("areaSqm"),
            netWeightKg = o.dbl("netWeightKg"), grossWeightKg = o.dbl("grossWeightKg"),
            grade = o.str("grade"), itemCode = o.str("itemCode"), batch = o.str("batch"), shade = o.str("shade"),
            mfgDate = o.str("mfgDate"), expDate = o.str("expDate"), shelfLifeMonths = o.int("shelfLifeMonths"),
            standards = o.optJSONArray("standards").strings().take(6), extraSpecs = specs,
            hasVietnameseLabel = o.bool("hasVietnameseLabel"),
            handwritten = o.optJSONArray("handwrittenFields").strings().take(6),
            rawText = raw.take(4000), aiSigns = o.optJSONArray("suspiciousSigns").strings().take(3),
            confidence = o.dbl("confidence"), barcodeTexts = o.optJSONArray("barcodes").strings().take(6),
            marks = o.optJSONArray("marks").let { arr ->
                if (arr == null) emptyList() else List(arr.length()) { i ->
                    val m = arr.optJSONObject(i)
                    (m?.str("mark") ?: "") to (m?.str("meaning") ?: "")
                }.filter { it.first.isNotBlank() }.distinctBy { it.first.lowercase() }.take(16)
            },
            marketNote = o.str("marketNote").take(300),
        )
    }
}
