package com.fws.smarttrace.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.R
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.model.AnalysisMode
import com.fws.smarttrace.model.CodeCheck
import com.fws.smarttrace.model.Finding
import com.fws.smarttrace.model.ScanResult
import com.fws.smarttrace.model.Severity
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.FindingText
import com.fws.smarttrace.ui.Group
import com.fws.smarttrace.ui.components.GhostButton
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.components.IconAction
import com.fws.smarttrace.ui.components.LabelValue
import com.fws.smarttrace.ui.components.ScoreRing
import com.fws.smarttrace.ui.components.ScreenSurface
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.components.SpeechBar
import com.fws.smarttrace.ui.components.TopBar
import com.fws.smarttrace.ui.components.severityColor
import com.fws.smarttrace.ui.components.severityIcon
import com.fws.smarttrace.ui.components.verdictColor
import com.fws.smarttrace.ui.theme.AppTheme

private fun fmtNum(d: Double): String = java.lang.String.format(java.util.Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

private fun speechParagraphs(ctx: Context, r: ScanResult): List<String> {
    val out = ArrayList<String>()
    val title = r.title.ifBlank { ctx.getString(R.string.hist_untitled) }
    out += title + ". " + ctx.getString(FindingText.verdictRes(r.verdict)) + ". " + ctx.getString(R.string.score_label, r.score.toString())
    out += ctx.getString(FindingText.verdictDescRes(r.verdict))
    val important = r.findings.filter { it.severity == Severity.BAD || it.severity == Severity.WARN }
    val rest = r.findings.filter { it.severity == Severity.OK || it.severity == Severity.INFO }
    (important + rest).take(10).forEach { out += FindingText.text(ctx, it) }
    out += ctx.getString(R.string.disclaimer)
    return out
}

@Composable
fun ResultScreen(vm: AppViewModel, result: ScanResult, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val items by vm.history.items.collectAsState()
    val fav = items.firstOrNull { it.id == result.id }?.favorite ?: false
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val paragraphs = remember(result.id) { speechParagraphs(ctx, result) }
    val tabs = listOf(
        R.string.res_tab_overview, R.string.res_tab_specs, R.string.res_tab_auth, R.string.res_tab_supply, R.string.res_tab_legal,
    )

    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                result.title.ifBlank { stringResource(R.string.hist_untitled) }, onBack = onBack,
                actions = {
                    IconAction(
                        if (fav) R.drawable.ic_heart_fill else R.drawable.ic_heart,
                        tint = if (fav) AppTheme.colors.gold else MaterialTheme.colorScheme.onBackground,
                    ) { vm.toggleFavorite(result.id, fav) }
                    IconAction(R.drawable.ic_trash) { confirmDelete = true }
                },
            )
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                contentColor = AppTheme.colors.gold,
            ) {
                tabs.forEachIndexed { i, res ->
                    Tab(
                        selected = tab == i, onClick = { tab = i },
                        text = { Text(stringResource(res), maxLines = 1) },
                        selectedContentColor = AppTheme.colors.gold,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (tab) {
                    0 -> OverviewTab(vm, result, paragraphs)
                    1 -> SpecsTab(result)
                    2 -> AuthTab(result)
                    3 -> SupplyTab(result)
                    else -> LegalTab(result)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.delete_one_confirm)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(result.id) }) { Text(stringResource(R.string.delete_item)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// ---------------------------------------------------------------- tabs

@Composable
private fun OverviewTab(vm: AppViewModel, r: ScanResult, paragraphs: List<String>) {
    val ctx = LocalContext.current
    SpeechBar(vm, paragraphs)

    val thumb = rememberThumb(r.id, 1)
    if (thumb != null) {
        Image(
            thumb, null,
            Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, AppTheme.colors.outline, RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
    }

    val vc = verdictColor(r.verdict)
    SmartCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(r.score, vc)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(FindingText.verdictRes(r.verdict)), color = vc,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.score_label, r.score.toString()),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(FindingText.verdictDescRes(r.verdict)), modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
        )
    }

    SmartCard {
        val l = r.label
        if (l.productName.isNotBlank()) LabelValue(stringResource(R.string.f_product), l.productName)
        if (l.brand.isNotBlank()) LabelValue(stringResource(R.string.f_brand), l.brand)
        LabelValue(stringResource(R.string.f_category), stringResource(FindingText.categoryRes(l.category)))
    }

    val issues = r.findings.filter { it.severity == Severity.BAD || it.severity == Severity.WARN }.take(4)
    if (issues.isNotEmpty()) {
        Text(stringResource(R.string.top_issues), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        FindingCard(issues)
    }
    DisclaimerCard()
}

@Composable
private fun SpecsTab(r: ScanResult) {
    val l = r.label
    val ctx = LocalContext.current
    val rows = ArrayList<Pair<String, String>>()
    fun add(res: Int, v: String?) { if (!v.isNullOrBlank()) rows += ctx.getString(res) to v }
    add(R.string.f_product, l.productName)
    add(R.string.f_brand, l.brand)
    add(R.string.f_category, stringResource(FindingText.categoryRes(l.category)))
    add(R.string.f_maker, l.manufacturer)
    add(R.string.f_origin, l.origin)
    add(R.string.f_address, l.address)
    add(R.string.f_phone, l.phone)
    add(R.string.f_size, l.size)
    add(R.string.f_qty, l.quantityPcs?.let { stringResource(R.string.qty_pcs, it.toString()) })
    add(R.string.f_area, l.areaSqm?.let { fmtNum(it) + " m²" })
    add(R.string.f_net, l.netWeightKg?.let { fmtNum(it) + " kg" })
    add(R.string.f_gross, l.grossWeightKg?.let { fmtNum(it) + " kg" })
    add(R.string.f_grade, l.grade)
    add(R.string.f_item, l.itemCode)
    add(R.string.f_shade, l.shade)
    add(R.string.f_batch, l.batch)
    add(R.string.f_mfg, l.mfgDate)
    add(R.string.f_exp, l.expDate)
    add(R.string.f_shelf, l.shelfLifeMonths?.let { stringResource(R.string.months_fmt, it.toString()) })
    add(R.string.f_standards, l.standards.joinToString(", "))
    l.extraSpecs.forEach { rows += it }
    add(R.string.f_visual, l.visualHints.joinToString(", "))

    SmartCard {
        if (rows.isEmpty()) {
            Text(stringResource(R.string.not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else rows.forEach { (k, v) -> LabelValue(k, v) }
    }

    if (l.rawText.isNotBlank()) {
        var open by rememberSaveable { mutableStateOf(false) }
        SmartCard(onClick = { open = !open }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.f_raw), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                )
                Ico(
                    R.drawable.ic_chevron, tint = AppTheme.colors.gold,
                    modifier = if (open) Modifier.rotate(90f) else Modifier,
                )
            }
            if (open) {
                Text(
                    l.rawText, modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    DisclaimerCard()
}

@Composable
private fun AuthTab(r: ScanResult) {
    Text(stringResource(R.string.sec_codes), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
    r.codes.forEach { CodeCard(it) }
    FindingCard(r.findings.filter { FindingText.group(it) == Group.AUTH })
    FindingCard(r.findings.filter { FindingText.group(it) == Group.QUALITY })
    DisclaimerCard()
}

private data class Step(val title: Int, val value: String, val severity: Severity)

@Composable
private fun SupplyTab(r: ScanResult) {
    val l = r.label
    val notFound = stringResource(R.string.not_found)
    val steps = ArrayList<Step>()

    val gtinCode = r.codes.firstOrNull { it.gtinValid == true }
    val originText = l.origin.ifBlank { gtinCode?.prefixIso?.let { FindingText.countryName(it) } ?: "" }
    steps += Step(R.string.sc_origin, originText.ifBlank { notFound }, if (l.origin.isNotBlank()) Severity.OK else Severity.INFO)

    val maker = l.manufacturer.ifBlank { l.brand }
    val makerText = listOf(maker, l.address, l.phone).filter { it.isNotBlank() }.joinToString("\n")
    steps += Step(R.string.sc_maker, makerText.ifBlank { notFound }, if (maker.isNotBlank()) Severity.OK else Severity.WARN)

    val trace = listOf(l.batch, l.shade, l.mfgDate, l.expDate).filter { it.isNotBlank() }.joinToString(" · ")
    steps += Step(R.string.sc_batch, trace.ifBlank { notFound }, if (trace.isNotBlank()) Severity.OK else Severity.WARN)

    val reg = gtinCode?.let { c ->
        val country = c.prefixIso?.let { FindingText.countryName(it) }
        listOfNotNull(c.gtin ?: c.value, country, c.lookup?.takeIf { it.found }?.let { it.name.ifBlank { it.brand } }).joinToString(" · ")
    }
    steps += Step(R.string.sc_registry, reg ?: notFound, if (reg != null) Severity.OK else Severity.INFO)

    if (Languages.isVietnamese() && l.hasVietnameseLabel != null) {
        steps += Step(R.string.sc_vn, if (l.hasVietnameseLabel == true) "✓" else notFound, if (l.hasVietnameseLabel == true) Severity.OK else Severity.WARN)
    }

    SmartCard {
        steps.forEachIndexed { i, s ->
            val c = severityColor(s.severity)
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(Modifier.width(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)).border(1.2.dp, c, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Ico(severityIcon(s.severity), tint = c, size = 16.dp) }
                    if (i < steps.lastIndex) {
                        Box(Modifier.width(2.dp).weight(1f).background(AppTheme.colors.outline))
                    }
                }
                Column(Modifier.padding(start = 12.dp, bottom = if (i < steps.lastIndex) 16.dp else 0.dp)) {
                    Text(stringResource(s.title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    FindingCard(r.findings.filter { FindingText.group(it) == Group.SUPPLY })
    Text(
        stringResource(R.string.sc_note), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LegalTab(r: ScanResult) {
    FindingCard(r.findings.filter { FindingText.group(it) == Group.LEGAL })
    Text(
        stringResource(R.string.legal_note), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DisclaimerCard()
}

// ---------------------------------------------------------------- pieces

@Composable
private fun DisclaimerCard() {
    SmartCard {
        Row(verticalAlignment = Alignment.Top) {
            Ico(R.drawable.ic_info, tint = AppTheme.colors.info)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FindingCard(findings: List<Finding>) {
    if (findings.isEmpty()) return
    val ctx = LocalContext.current
    SmartCard {
        findings.forEachIndexed { i, f ->
            val c = severityColor(f.severity)
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(c.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Ico(severityIcon(f.severity), tint = c, size = 15.dp) }
                Spacer(Modifier.width(12.dp))
                Text(
                    FindingText.text(ctx, f), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (i < findings.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.outline.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun CodeCard(c: CodeCheck) {
    val ctx = LocalContext.current
    SmartCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                c.format, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.gold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppTheme.colors.gold.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (c.source == "ai") R.string.code_source_ai else R.string.code_source_scan),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            c.gtin ?: c.value, modifier = Modifier.padding(top = 8.dp),
            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
        )
        c.lookup?.takeIf { it.found }?.let { lk ->
            Column(Modifier.padding(top = 8.dp)) {
                if (lk.name.isNotBlank()) LabelValue(stringResource(R.string.f_product), lk.name)
                if (lk.brand.isNotBlank()) LabelValue(stringResource(R.string.f_brand), lk.brand)
                if (lk.quantity.isNotBlank()) LabelValue(stringResource(R.string.f_qty), lk.quantity)
                LabelValue("DB", lk.source)
            }
        }
        val gtinToCheck = c.gtin
        if (gtinToCheck != null && c.gtinValid == true) {
            GhostButton(
                stringResource(R.string.open_gs1), modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), icon = R.drawable.ic_shield,
            ) {
                try {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gs1.org/services/verified-by-gs1/results?gtin=" + Uri.encode(gtinToCheck)))
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                    )
                } catch (e: ActivityNotFoundException) { /* no browser installed */ }
            }
        }
        c.url?.let { u ->
            Text(u.host.ifBlank { u.url }, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (u.url.startsWith("http://", true) || u.url.startsWith("https://", true)) {
                GhostButton(
                    stringResource(R.string.open_link), modifier = Modifier.padding(top = 10.dp), icon = R.drawable.ic_link,
                ) {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url)).addCategory(Intent.CATEGORY_BROWSABLE))
                    } catch (e: ActivityNotFoundException) { /* no browser installed */ }
                }
            }
        }
    }
}
