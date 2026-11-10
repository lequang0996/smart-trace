package com.fws.smarttrace.verify

import com.fws.smarttrace.model.LookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks a barcode up in open product databases. Only the digits of the barcode are sent.
 * "Not found" is never treated as proof of a fake: these databases are incomplete.
 */
object ProductLookup {
    private const val UA = "SmartTrace/1.0 (17eathday@gmail.com)"
    private val OPEN_FACTS = listOf(
        "Open Food Facts" to "https://world.openfoodfacts.org",
        "Open Beauty Facts" to "https://world.openbeautyfacts.org",
        "Open Products Facts" to "https://world.openproductsfacts.org",
    )

    private fun http(url: String): Pair<Int, String>? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8_000
            c.readTimeout = 8_000
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            val s = if (code in 200..299) c.inputStream else c.errorStream
            code to (s?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    suspend fun lookup(gtin: String): LookupResult = withContext(Dispatchers.IO) {
        var reached = false
        for ((name, base) in OPEN_FACTS) {
            val r = http("$base/api/v2/product/$gtin.json?fields=product_name,brands,quantity") ?: continue
            reached = true
            val o = runCatching { JSONObject(r.second) }.getOrNull() ?: continue
            if (o.optInt("status") == 1) {
                val p = o.optJSONObject("product")
                val title = p?.optString("product_name", "")?.trim().orEmpty()
                val brand = p?.optString("brands", "")?.trim().orEmpty()
                if (title.isNotEmpty() || brand.isNotEmpty()) {
                    return@withContext LookupResult(name, true, title, brand, p?.optString("quantity", "")?.trim().orEmpty())
                }
            }
        }
        if (gtin.length == 12 || gtin.length == 13) {
            val r = http("https://api.upcitemdb.com/prod/trial/lookup?upc=$gtin")
            if (r != null) {
                reached = true
                val o = runCatching { JSONObject(r.second) }.getOrNull()
                val items = o?.optJSONArray("items")
                if (o?.optString("code") == "OK" && items != null && items.length() > 0) {
                    val it0 = items.getJSONObject(0)
                    return@withContext LookupResult(
                        "UPCitemdb", true,
                        it0.optString("title", "").trim(), it0.optString("brand", "").trim(), it0.optString("size", "").trim()
                    )
                }
            }
        }
        val names = OPEN_FACTS.joinToString(", ") { it.first }
        if (reached) LookupResult(names, false) else LookupResult(names, false, error = true)
    }
}
