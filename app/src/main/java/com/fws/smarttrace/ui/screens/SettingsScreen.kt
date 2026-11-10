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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

private val SPEEDS = listOf(1f, 1.5f, 2f, 3f, 4f)
private fun speedLabel(s: Float): String = (if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×"

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
                    SelectChip(stringResource(R.string.lang_system), currentLang.isEmpty()) { vm.setLanguage("") }
                    Languages.supported.forEach { tag ->
                        SelectChip(Languages.nativeNames.getValue(tag), currentLang == tag) { vm.setLanguage(tag) }
                    }
                }
            }

            SmartCard {
                Text(stringResource(R.string.set_tts_speed), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
                FlowRow(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SPEEDS.forEach { s -> SelectChip(speedLabel(s), st.ttsSpeed == s) { vm.setSpeed(s) } }
                }
            }

            SmartCard(onClick = onKeys) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ico(R.drawable.ic_key, tint = AppTheme.colors.gold)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_keys), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(R.string.set_keys_count, st.keys.size.toString()) + " · " + st.model,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Ico(R.drawable.ic_chevron, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SmartCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_online), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(R.string.set_online_desc),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = st.online, onCheckedChange = { vm.setOnline(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Navy, checkedTrackColor = AppTheme.colors.gold),
                    )
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
