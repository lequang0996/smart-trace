package com.fws.smarttrace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.R
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.theme.AppTheme

private val SPEEDS = listOf(1f, 1.5f, 2f, 3f, 4f)

private fun speedLabel(s: Float): String = (if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×"

/** Listen / Stop button plus speed chips (default 2×, up to 4×). Shows the paragraph being read. */
@Composable
fun SpeechBar(vm: AppViewModel, paragraphs: List<String>, modifier: Modifier = Modifier) {
    val st by vm.speech.state.collectAsState()
    val settings by vm.settings.collectAsState()
    DisposableEffect(Unit) { onDispose { vm.speech.stop() } }

    SmartCard(modifier) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (st.playing) {
                GhostButton(stringResource(R.string.tts_stop), icon = R.drawable.ic_stop, modifier = Modifier.weight(1f)) { vm.speech.stop() }
            } else {
                GoldButton(stringResource(R.string.tts_listen), icon = R.drawable.ic_speaker, modifier = Modifier.weight(1f)) {
                    vm.speech.clearMissingFlag()
                    vm.speech.speak(paragraphs, Languages.effectiveLocale(), settings.ttsSpeed)
                }
            }
        }
        Row(
            Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.tts_speed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SPEEDS.forEach { s ->
                SelectChip(speedLabel(s), selected = settings.ttsSpeed == s) { vm.setSpeed(s) }
            }
        }
        if (st.missingVoice) {
            Text(
                stringResource(R.string.tts_unavailable), modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.warn,
            )
        }
        val current = paragraphs.getOrNull(st.index)
        if (st.playing && current != null) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(stringResource(R.string.tts_reading), style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.gold)
                Text(current, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
