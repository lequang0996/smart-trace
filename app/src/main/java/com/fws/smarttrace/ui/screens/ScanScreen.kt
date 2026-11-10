package com.fws.smarttrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.smarttrace.R
import com.fws.smarttrace.Stage
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.ScanState
import com.fws.smarttrace.ui.components.GhostButton
import com.fws.smarttrace.ui.components.GoldButton
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.components.SelectChip
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.theme.AppTheme
import com.fws.smarttrace.ui.theme.Gold

private fun stageText(s: Stage): Int = when (s) {
    Stage.PREPARE -> R.string.stage_prepare
    Stage.CODES -> R.string.stage_code
    Stage.READ -> R.string.stage_read
    Stage.AI -> R.string.stage_ai
    Stage.VERIFY -> R.string.stage_verify
    Stage.SCORE -> R.string.stage_score
}

@Composable
fun ScanScreen(vm: AppViewModel, onCamera: () -> Unit, onGallery: () -> Unit, onOpenKeys: () -> Unit) {
    val st by vm.settings.collectAsState()
    val scan by vm.scan.collectAsState()
    var askConsent by remember { mutableStateOf(false) }
    val busy = scan is ScanState.Running

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Hero()

        SmartCard {
            Text(stringResource(R.string.scan_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectChip(stringResource(R.string.mode_offline), selected = !st.useGemini, icon = R.drawable.ic_shield) {
                    vm.setUseGemini(false)
                }
                SelectChip(stringResource(R.string.mode_gemini), selected = st.useGemini, icon = R.drawable.ic_sparkle) {
                    if (!st.consent) askConsent = true else vm.setUseGemini(true)
                }
            }
            Text(
                stringResource(if (st.useGemini) R.string.mode_hint_gemini else R.string.mode_hint_offline),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (st.useGemini && st.keys.isEmpty()) {
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.need_key), modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.warn,
                    )
                    Spacer(Modifier.width(10.dp))
                    GhostButton(stringResource(R.string.btn_add_key), icon = R.drawable.ic_key, onClick = onOpenKeys)
                }
            }
        }

        when (val s = scan) {
            is ScanState.Running -> SmartCard {
                Text(stringResource(stageText(s.stage)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = AppTheme.colors.gold, trackColor = AppTheme.colors.outline,
                )
            }
            is ScanState.Failed -> SmartCard(onClick = { vm.dismissError() }) {
                val msg = if (s.resId != null) stringResource(s.resId) else stringResource(R.string.err_generic, s.message ?: "")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ico(R.drawable.ic_alert, tint = AppTheme.colors.bad)
                    Spacer(Modifier.width(10.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            ScanState.Idle -> {}
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(stringResource(R.string.btn_camera), modifier = Modifier.weight(1f), icon = R.drawable.ic_camera, enabled = !busy, onClick = onCamera)
            GhostButton(stringResource(R.string.btn_gallery), modifier = Modifier.weight(1f), icon = R.drawable.ic_image, enabled = !busy, onClick = onGallery)
        }

        SmartCard {
            Text(stringResource(R.string.tips_title), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
            Text(
                stringResource(R.string.tips_body), modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (askConsent) {
        AlertDialog(
            onDismissRequest = { askConsent = false },
            title = { Text(stringResource(R.string.consent_title)) },
            text = { Text(stringResource(R.string.consent_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askConsent = false
                    vm.setConsent(true)
                    vm.setUseGemini(true)
                }) { Text(stringResource(R.string.consent_agree)) }
            },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Hero() {
    val c = AppTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.hero).padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Gold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Ico(R.drawable.ic_scan_code, tint = Gold, size = 34.dp) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.app_name), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    fontSize = 30.sp, color = c.heroText,
                )
                Text(
                    stringResource(R.string.tagline), fontSize = 13.sp, color = c.heroText.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
