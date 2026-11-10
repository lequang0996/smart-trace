package com.fws.smarttrace.model

import org.json.JSONArray
import org.json.JSONObject

enum class Verdict { CONSISTENT, CHECK, SUSPICIOUS, INSUFFICIENT }
enum class Severity { OK, INFO, WARN, BAD }
enum class AnalysisMode { OFFLINE, GEMINI }

internal fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else List(length()) { optString(it, "") }.filter { it.isNotBlank() }

internal fun JSONObject.str(k: String): String = if (!has(k) || isNull(k)) "" else optString(k, "").trim()

internal fun JSONObject.dbl(k: String): Double? =
    if (!has(k) || isNull(k)) null else optDouble(k, Double.NaN).takeIf { !it.isNaN() }

internal fun JSONObject.int(k: String): Int? = dbl(k)?.toInt()

internal fun JSONObject.bool(k: String): Boolean? =
    if (!has(k) || isNull(k)) null else when (val v = opt(k)) {
        is Boolean -> v
        is String -> v.equals("true", true)
        else -> null
    }

/** A single line of the consistency report. Text is looked up by [code] at display time so it follows the UI language. */
data class Finding(val code: String, val severity: Severity, val args: List<String> = emptyList()) {
    fun toJson(): JSONObject = JSONObject().put("c", code).put("s", severity.name).put("a", JSONArray(args))

    companion object {
        fun fromJson(o: JSONObject) = Finding(
            o.getString("c"),
            runCatching { Severity.valueOf(o.getString("s")) }.getOrDefault(Severity.INFO),
            o.optJSONArray("a").strings()
        )
    }
}

data class ExtractedLabel(
    val productName: String = "",
    val brand: String = "",
    val category: String = "other",
    val manufacturer: String = "",
    val origin: String = "",
    val originIso: String = "",
    val address: String = "",
    val phone: String = "",
    val size: String = "",
    val quantityPcs: Int? = null,
    val areaSqm: Double? = null,
    val netWeightKg: Double? = null,
    val grossWeightKg: Double? = null,
    val grade: String = "",
    val itemCode: String = "",
    val batch: String = "",
    val shade: String = "",
    val mfgDate: String = "",
    val expDate: String = "",
    val shelfLifeMonths: Int? = null,
    val standards: List<String> = emptyList(),
    val extraSpecs: List<Pair<String, String>> = emptyList(),
    val hasVietnameseLabel: Boolean? = null,
    val handwritten: List<String> = emptyList(),
    val rawText: String = "",
    val aiSigns: List<String> = emptyList(),
    val confidence: Double? = null,
    val barcodeTexts: List<String> = emptyList(),
    val visualHints: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("productName", productName); put("brand", brand); put("category", category)
        put("manufacturer", manufacturer); put("origin", origin); put("originIso", originIso); put("address", address); put("phone", phone)
        put("size", size)
        quantityPcs?.let { put("quantityPcs", it) }
        areaSqm?.let { put("areaSqm", it) }
        netWeightKg?.let { put("netWeightKg", it) }
        grossWeightKg?.let { put("grossWeightKg", it) }
        put("grade", grade); put("itemCode", itemCode); put("batch", batch); put("shade", shade)
        put("mfgDate", mfgDate); put("expDate", expDate)
        shelfLifeMonths?.let { put("shelfLifeMonths", it) }
        put("standards", JSONArray(standards))
        put("extraSpecs", JSONArray().also { a -> extraSpecs.forEach { (l, v) -> a.put(JSONObject().put("l", l).put("v", v)) } })
        hasVietnameseLabel?.let { put("hasVn", it) }
        put("handwritten", JSONArray(handwritten))
        put("rawText", rawText)
        put("aiSigns", JSONArray(aiSigns))
        confidence?.let { put("confidence", it) }
        put("barcodeTexts", JSONArray(barcodeTexts))
        put("visualHints", JSONArray(visualHints))
    }

    companion object {
        fun fromJson(o: JSONObject): ExtractedLabel {
            val specs = o.optJSONArray("extraSpecs")
            val extra = if (specs == null) emptyList() else List(specs.length()) { i ->
                val s = specs.optJSONObject(i)
                (s?.optString("l") ?: "") to (s?.optString("v") ?: "")
            }.filter { it.first.isNotBlank() && it.second.isNotBlank() }
            return ExtractedLabel(
                productName = o.str("productName"), brand = o.str("brand"),
                category = o.str("category").ifBlank { "other" },
                manufacturer = o.str("manufacturer"), origin = o.str("origin"), originIso = o.str("originIso"),
                address = o.str("address"), phone = o.str("phone"), size = o.str("size"),
                quantityPcs = o.int("quantityPcs"), areaSqm = o.dbl("areaSqm"),
                netWeightKg = o.dbl("netWeightKg"), grossWeightKg = o.dbl("grossWeightKg"),
                grade = o.str("grade"), itemCode = o.str("itemCode"), batch = o.str("batch"), shade = o.str("shade"),
                mfgDate = o.str("mfgDate"), expDate = o.str("expDate"), shelfLifeMonths = o.int("shelfLifeMonths"),
                standards = o.optJSONArray("standards").strings(), extraSpecs = extra,
                hasVietnameseLabel = o.bool("hasVn"), handwritten = o.optJSONArray("handwritten").strings(),
                rawText = o.str("rawText"), aiSigns = o.optJSONArray("aiSigns").strings(),
                confidence = o.dbl("confidence"), barcodeTexts = o.optJSONArray("barcodeTexts").strings(),
                visualHints = o.optJSONArray("visualHints").strings(),
            )
        }
    }
}

data class LookupResult(
    val source: String,
    val found: Boolean,
    val name: String = "",
    val brand: String = "",
    val quantity: String = "",
    val error: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().put("source", source).put("found", found).put("name", name)
        .put("brand", brand).put("quantity", quantity).put("error", error)

    companion object {
        fun fromJson(o: JSONObject) = LookupResult(
            o.str("source"), o.optBoolean("found"), o.str("name"), o.str("brand"), o.str("quantity"), o.optBoolean("error")
        )
    }
}

data class UrlInfo(val url: String, val host: String, val https: Boolean, val flags: List<String>) {
    fun toJson(): JSONObject = JSONObject().put("url", url).put("host", host).put("https", https).put("flags", JSONArray(flags))

    companion object {
        fun fromJson(o: JSONObject) = UrlInfo(o.str("url"), o.str("host"), o.optBoolean("https"), o.optJSONArray("flags").strings())
    }
}

/** kind: GTIN | URL | TEXT. source: scan | ai. */
data class CodeCheck(
    val value: String,
    val format: String,
    val source: String,
    val kind: String,
    val gtin: String? = null,
    val gtinValid: Boolean? = null,
    val prefixIso: String? = null,
    val lookup: LookupResult? = null,
    val url: UrlInfo? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("value", value); put("format", format); put("source", source); put("kind", kind)
        gtin?.let { put("gtin", it) }
        gtinValid?.let { put("gtinValid", it) }
        prefixIso?.let { put("prefixIso", it) }
        lookup?.let { put("lookup", it.toJson()) }
        url?.let { put("url", it.toJson()) }
    }

    companion object {
        fun fromJson(o: JSONObject) = CodeCheck(
            value = o.str("value"), format = o.str("format"), source = o.str("source"), kind = o.str("kind"),
            gtin = o.str("gtin").ifBlank { null }, gtinValid = o.bool("gtinValid"),
            prefixIso = o.str("prefixIso").ifBlank { null },
            lookup = o.optJSONObject("lookup")?.let { LookupResult.fromJson(it) },
            url = o.optJSONObject("url")?.let { UrlInfo.fromJson(it) },
        )
    }
}

data class ScanResult(
    val id: String,
    val createdAt: Long,
    val mode: AnalysisMode,
    val model: String,
    val label: ExtractedLabel,
    val codes: List<CodeCheck>,
    val findings: List<Finding>,
    val verdict: Verdict,
    val score: Int,
) {
    val title: String get() = label.productName.ifBlank { label.brand }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("createdAt", createdAt); put("mode", mode.name); put("model", model)
        put("label", label.toJson())
        put("codes", JSONArray().also { a -> codes.forEach { a.put(it.toJson()) } })
        put("findings", JSONArray().also { a -> findings.forEach { a.put(it.toJson()) } })
        put("verdict", verdict.name); put("score", score)
    }

    companion object {
        fun fromJson(o: JSONObject): ScanResult {
            val codes = o.optJSONArray("codes")
            val finds = o.optJSONArray("findings")
            return ScanResult(
                id = o.getString("id"), createdAt = o.optLong("createdAt"),
                mode = runCatching { AnalysisMode.valueOf(o.getString("mode")) }.getOrDefault(AnalysisMode.OFFLINE),
                model = o.str("model"),
                label = ExtractedLabel.fromJson(o.getJSONObject("label")),
                codes = if (codes == null) emptyList() else List(codes.length()) { CodeCheck.fromJson(codes.getJSONObject(it)) },
                findings = if (finds == null) emptyList() else List(finds.length()) { Finding.fromJson(finds.getJSONObject(it)) },
                verdict = runCatching { Verdict.valueOf(o.getString("verdict")) }.getOrDefault(Verdict.CHECK),
                score = o.optInt("score"),
            )
        }
    }
}

data class HistoryItem(
    val id: String,
    val createdAt: Long,
    val title: String,
    val brand: String,
    val category: String,
    val verdict: Verdict,
    val score: Int,
    val favorite: Boolean,
)
