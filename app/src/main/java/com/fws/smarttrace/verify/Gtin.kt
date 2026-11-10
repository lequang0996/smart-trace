package com.fws.smarttrace.verify

object Gtin {
    fun isValid(code: String): Boolean {
        if (code.length !in intArrayOf(8, 12, 13, 14) || !code.all { it in '0'..'9' }) return false
        val d = code.map { it - '0' }
        var sum = 0
        for (i in d.size - 2 downTo 0) {
            val posFromRight = d.size - 1 - i
            sum += d[i] * if (posFromRight % 2 == 1) 3 else 1
        }
        return (10 - sum % 10) % 10 == d.last()
    }

    /** The 3-digit GS1 prefix in EAN-13 form. */
    fun prefix3(code: String): String? = when (code.length) {
        13, 8 -> code.substring(0, 3)
        12 -> "0" + code.substring(0, 2)
        14 -> code.substring(1, 4)
        else -> null
    }

    /** Country (ISO 3166 alpha-2) the GS1 prefix is registered in, or null if unknown / not a country. */
    fun countryIso(code: String): String? {
        val p = prefix3(code)?.toIntOrNull() ?: return null
        return RANGES.firstOrNull { p in it.first }?.second
    }

    private val RANGES: List<Pair<IntRange, String>> = listOf(
        0..19 to "US", 30..39 to "US", 60..139 to "US",
        300..379 to "FR", 400..440 to "DE", 450..459 to "JP", 490..499 to "JP", 460..469 to "RU",
        471..471 to "TW", 480..480 to "PH", 489..489 to "HK", 500..509 to "GB", 520..521 to "GR",
        539..539 to "IE", 540..549 to "BE", 560..560 to "PT", 570..579 to "DK", 590..590 to "PL",
        600..601 to "ZA", 640..649 to "FI", 690..699 to "CN", 700..709 to "NO", 729..729 to "IL",
        730..739 to "SE", 750..750 to "MX", 754..755 to "CA", 760..769 to "CH", 780..780 to "CL",
        789..790 to "BR", 800..839 to "IT", 840..849 to "ES", 868..869 to "TR", 870..879 to "NL",
        880..880 to "KR", 884..884 to "KH", 885..885 to "TH", 888..888 to "SG", 890..890 to "IN",
        893..893 to "VN", 896..896 to "PK", 899..899 to "ID", 900..919 to "AT", 930..939 to "AU",
        940..949 to "NZ", 955..955 to "MY", 958..958 to "MO",
    )

    private val ORIGIN_ALIASES = listOf(
        "CN" to listOf("china", "中国", "trung quốc", "trung quoc", "prc"),
        "VN" to listOf("vietnam", "viet nam", "việt nam"),
        "TH" to listOf("thailand", "thái lan", "thai lan"),
        "JP" to listOf("japan", "日本", "nhật bản", "nhat ban"),
        "KR" to listOf("korea", "한국", "hàn quốc", "han quoc"),
        "TW" to listOf("taiwan", "đài loan", "dai loan"),
        "MY" to listOf("malaysia"),
        "ID" to listOf("indonesia"),
        "IN" to listOf("india", "ấn độ", "an do"),
        "US" to listOf("usa", "u.s.a", "united states", "america", "mỹ"),
        "DE" to listOf("germany", "deutschland", "đức"),
        "IT" to listOf("italy", "italia"),
        "ES" to listOf("spain", "españa", "tây ban nha"),
        "FR" to listOf("france", "pháp"),
        "GB" to listOf("united kingdom", "u.k", "england"),
        "SG" to listOf("singapore"),
    )

    /** Maps free-text origin (e.g. "Made in China") to an ISO country code when it is recognisable. */
    fun originToIso(origin: String): String? {
        val o = origin.lowercase()
        if (o.isBlank()) return null
        return ORIGIN_ALIASES.firstOrNull { (_, names) -> names.any { o.contains(it) } }?.first
    }
}
