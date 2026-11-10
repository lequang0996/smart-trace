package com.fws.smarttrace.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.BuildConfig
import com.fws.smarttrace.R
import com.fws.smarttrace.ads.AdsManager
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.components.BannerAd
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.components.SelectChip
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.components.TopBar
import com.fws.smarttrace.ui.theme.AppTheme
import com.fws.smarttrace.ui.theme.Navy

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val MIN_SPEED = 1f
private const val MAX_SPEED = 4f
private const val SPEED_STEP = 0.25f

/** 1× · 1.25× · 1.5× · 1.75× … 4× */
private fun speedLabel(s: Float): String {
    val snapped = Math.round(s / SPEED_STEP) * SPEED_STEP
    return java.lang.String.format(java.util.Locale.US, "%.2f", snapped).trimEnd('0').trimEnd('.') + "×"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onGuide: () -> Unit, onKeys: () -> Unit) {
    val ctx = LocalContext.current
    val st by vm.settings.collectAsState()
    val privacyOptions by AdsManager.privacyOptionsRequired.collectAsState()
    val currentLang = Languages.currentTag()

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.set_title), onBack = null)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SmartCard {
                Text(stringResource(R.string.set_appearance), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(stringResource(R.string.theme_system), st.themeMode == 0) { vm.setTheme(0) }
                    SelectChip(stringResource(R.string.theme_light), st.themeMode == 1) { vm.setTheme(1) }
                    SelectChip(stringResource(R.string.theme_dark), st.themeMode == 2) { vm.setTheme(2) }
                }
            }

            SmartCard {
                Text(stringResource(R.string.set_language), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
                FlowRow(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Languages.supported.forEach { tag ->
                        SelectChip(Languages.nativeNames.getValue(tag), currentLang == tag) { vm.setLanguage(tag) }
                    }
                }
            }

            SmartCard {
                Text(stringResource(R.string.set_tts_speed), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
                // Slider moves in 0.25x steps; the speed is applied when the finger lifts so reading
                // is not restarted on every tick.
                var sliding by remember { mutableFloatStateOf(st.ttsSpeed) }
                LaunchedEffect(st.ttsSpeed) { sliding = st.ttsSpeed }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = sliding,
                        onValueChange = { sliding = it },
                        onValueChangeFinished = { vm.setSpeed(sliding) },
                        valueRange = MIN_SPEED..MAX_SPEED,
                        steps = ((MAX_SPEED - MIN_SPEED) / SPEED_STEP).toInt() - 1,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AppTheme.colors.gold,
                            activeTrackColor = AppTheme.colors.gold,
                            inactiveTrackColor = AppTheme.colors.outline,
                            activeTickColor = Navy.copy(alpha = 0.55f),
                            inactiveTickColor = AppTheme.colors.gold.copy(alpha = 0.45f),
                        ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        speedLabel(sliding), modifier = Modifier.width(52.dp),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            SmartCard(onClick = onKeys) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ico(R.drawable.ic_key, tint = AppTheme.colors.gold)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_keys), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(R.string.set_keys_count, st.keys.size.toString()),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Ico(R.drawable.ic_chevron, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SmartCard {
                LinkRow(R.drawable.ic_book, stringResource(R.string.set_guide)) { onGuide() }
                LinkRow(R.drawable.ic_shield, stringResource(R.string.set_privacy)) {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL)))
                    } catch (e: ActivityNotFoundException) { }
                }
                if (privacyOptions) {
                    LinkRow(R.drawable.ic_sliders, stringResource(R.string.set_ads_privacy)) {
                        ctx.findActivity()?.let { AdsManager.showPrivacyOptions(it) }
                    }
                }
            }

            Text(
                stringResource(R.string.set_version, BuildConfig.VERSION_NAME),
                modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BannerAd()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LinkRow(icon: Int, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ico(icon, tint = AppTheme.colors.gold)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Ico(R.drawable.ic_chevron, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
