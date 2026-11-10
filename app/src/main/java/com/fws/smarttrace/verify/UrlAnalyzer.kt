package com.fws.smarttrace.verify

import android.net.Uri
import com.fws.smarttrace.model.UrlInfo

object UrlAnalyzer {
    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rebrand.ly", "s.id",
        "shorturl.at", "tiny.cc", "buff.ly", "rb.gy", "t.ly", "v.gd", "lnkd.in", "qr.ae", "bl.ink",
    )
    private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val GS1_PATH = Regex("""/01/(\d{14})""")

    /** Returns the link facts plus the GTIN if the link is a GS1 Digital Link. The page is never fetched. */
    fun analyze(raw: String): Pair<UrlInfo, String?> {
        val text = raw.trim()
        val uri = runCatching { Uri.parse(text) }.getOrNull()
        val host = (uri?.host ?: "").lowercase().removePrefix("www.")
        val flags = ArrayList<String>()
        if (uri?.scheme.equals("http", ignoreCase = true)) flags += "http"
        if (IPV4.matches(host)) flags += "ip"
        if (host.split('.').any { it.startsWith("xn--") }) flags += "puny"
        if (host in SHORTENERS) flags += "short"
        val g14 = GS1_PATH.find(uri?.path ?: "")?.groupValues?.get(1)
        val gtin = g14?.let { if (it.startsWith("0")) it.substring(1) else it }
        return UrlInfo(text.take(300), host, uri?.scheme.equals("https", ignoreCase = true), flags) to gtin
    }
}
