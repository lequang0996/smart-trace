package com.fws.smarttrace.ui

import android.content.Context
import com.fws.smarttrace.R
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.model.Finding
import com.fws.smarttrace.model.Verdict
import java.util.Locale

enum class Group { AUTH, SUPPLY, LEGAL, QUALITY }

/** Maps finding codes (stored in history) to localized sentences and to the tab they belong to. */
object FindingText {
    private class Spec(val res: Int, val group: Group)

    private val specs: Map<String, Spec> = mapOf(
        "AREA_OK" to Spec(R.string.fd_area_ok, Group.AUTH),
        "AREA_DIFF" to Spec(R.string.fd_area_diff, Group.AUTH),
        "NO_MAKER" to Spec(R.string.fd_no_maker, Group.SUPPLY),
        "MAKER_OK" to Spec(R.string.fd_maker_ok, Group.SUPPLY),
        "CONTACT_OK" to Spec(R.string.fd_contact_ok, Group.SUPPLY),
        "CONTACT_MISSING" to Spec(R.string.fd_contact_missing, Group.SUPPLY),
        "TRACE_OK" to Spec(R.string.fd_trace_ok, Group.SUPPLY),
        "TRACE_MISSING" to Spec(R.string.fd_trace_missing, Group.SUPPLY),
        "PREFIX" to Spec(R.string.fd_prefix, Group.SUPPLY),
        "PREFIX_DIFF" to Spec(R.string.fd_prefix_diff, Group.SUPPLY),
        "EXP_OK" to Spec(R.string.fd_exp_ok, Group.LEGAL),
        "EXP_SOON" to Spec(R.string.fd_exp_soon, Group.LEGAL),
        "EXP_PAST" to Spec(R.string.fd_exp_past, Group.LEGAL),
        "SHELF" to Spec(R.string.fd_shelf, Group.LEGAL),
        "VN_OK" to Spec(R.string.fd_vn_ok, Group.LEGAL),
        "VN_MISSING" to Spec(R.string.fd_vn_missing, Group.LEGAL),
        "STD" to Spec(R.string.fd_std, Group.LEGAL),
        "STD_NONE" to Spec(R.string.fd_std_none, Group.LEGAL),
        "GRADE" to Spec(R.string.fd_grade, Group.LEGAL),
        "HAND" to Spec(R.string.fd_hand, Group.AUTH),
        "AI_SIGN" to Spec(R.string.fd_ai_sign, Group.AUTH),
        "AI_LOWCONF" to Spec(R.string.fd_ai_lowconf, Group.QUALITY),
        "OCR_POOR" to Spec(R.string.fd_ocr_poor, Group.QUALITY),
        "AI_FALLBACK" to Spec(R.string.fd_ai_fallback, Group.QUALITY),
        "BLUR" to Spec(R.string.fd_blur, Group.QUALITY),
        "DARK" to Spec(R.string.fd_dark, Group.QUALITY),
        "LOWTEXT" to Spec(R.string.fd_lowtext, Group.QUALITY),
        "NO_CODE" to Spec(R.string.fd_no_code, Group.AUTH),
        "AI_BARCODE" to Spec(R.string.fd_ai_barcode, Group.AUTH),
        "GTIN_OK" to Spec(R.string.fd_gtin_ok, Group.AUTH),
        "GTIN_BAD" to Spec(R.string.fd_gtin_bad, Group.AUTH),
        "GS1_VERIFY" to Spec(R.string.fd_gs1_verify, Group.AUTH),
        "DB_FOUND" to Spec(R.string.fd_db_found, Group.AUTH),
        "DB_NONE" to Spec(R.string.fd_db_none, Group.AUTH),
        "DB_OFFLINE" to Spec(R.string.fd_db_offline, Group.AUTH),
        "DB_BRAND_OK" to Spec(R.string.fd_db_brand_ok, Group.AUTH),
        "DB_BRAND_MISS" to Spec(R.string.fd_db_brand_miss, Group.AUTH),
        "DB_NAME_OK" to Spec(R.string.fd_db_name_ok, Group.AUTH),
        "DB_NAME_PART" to Spec(R.string.fd_db_name_part, Group.AUTH),
        "DB_NAME_DIFF" to Spec(R.string.fd_db_name_diff, Group.AUTH),
        "DB_QTY_OK" to Spec(R.string.fd_db_qty_ok, Group.AUTH),
        "DB_QTY_DIFF" to Spec(R.string.fd_db_qty_diff, Group.AUTH),
        "URL" to Spec(R.string.fd_url, Group.AUTH),
        "URL_HTTP" to Spec(R.string.fd_url_http, Group.AUTH),
        "URL_SHORT" to Spec(R.string.fd_url_short, Group.AUTH),
        "URL_IP" to Spec(R.string.fd_url_ip, Group.AUTH),
        "URL_PUNY" to Spec(R.string.fd_url_puny, Group.AUTH),
        "GS1LINK" to Spec(R.string.fd_gs1link, Group.AUTH),
        "QR_TEXT" to Spec(R.string.fd_qr_text, Group.AUTH),
        "CODE_TEXT" to Spec(R.string.fd_code_text, Group.AUTH),
        "IMEI_OK" to Spec(R.string.fd_imei_ok, Group.AUTH),
        "IMEI_BAD" to Spec(R.string.fd_imei_bad, Group.AUTH),
        "MARKS" to Spec(R.string.fd_marks, Group.LEGAL),
    )

    fun group(f: Finding): Group = specs[f.code]?.group ?: Group.AUTH

    fun countryName(iso: String): String = runCatching {
        Locale.Builder().setRegion(iso).build().getDisplayCountry(Languages.effectiveLocale()).ifBlank { iso }
    }.getOrDefault(iso)

    fun text(ctx: Context, f: Finding): String {
        val spec = specs[f.code] ?: return f.code
        val args = f.args.map { if (it.startsWith("iso:")) countryName(it.removePrefix("iso:")) else it }
        return ctx.getString(spec.res, *args.toTypedArray())
    }

    fun categoryRes(c: String): Int = when (c) {
        "tile" -> R.string.cat_tile
        "adhesive" -> R.string.cat_adhesive
        "paint" -> R.string.cat_paint
        "cement" -> R.string.cat_cement
        "food" -> R.string.cat_food
        "cosmetic" -> R.string.cat_cosmetic
        "electronics" -> R.string.cat_electronics
        else -> R.string.cat_other
    }

    fun verdictRes(v: Verdict): Int = when (v) {
        Verdict.CONSISTENT -> R.string.verdict_consistent
        Verdict.CHECK -> R.string.verdict_check
        Verdict.SUSPICIOUS -> R.string.verdict_suspicious
        Verdict.INSUFFICIENT -> R.string.verdict_insufficient
    }

    fun verdictDescRes(v: Verdict): Int = when (v) {
        Verdict.CONSISTENT -> R.string.verdict_desc_consistent
        Verdict.CHECK -> R.string.verdict_desc_check
        Verdict.SUSPICIOUS -> R.string.verdict_desc_suspicious
        Verdict.INSUFFICIENT -> R.string.verdict_desc_insufficient
    }
}
