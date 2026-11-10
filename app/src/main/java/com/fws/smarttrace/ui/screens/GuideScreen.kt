package com.fws.smarttrace.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.R
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.components.GoldButton
import com.fws.smarttrace.ui.components.ScreenSurface
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.components.SpeechBar
import com.fws.smarttrace.ui.components.TopBar
import com.fws.smarttrace.ui.theme.AppTheme

private val SECTIONS = listOf(
    R.string.g1_t to R.string.g1_b, R.string.g2_t to R.string.g2_b, R.string.g3_t to R.string.g3_b,
    R.string.g4_t to R.string.g4_b, R.string.g5_t to R.string.g5_b, R.string.g6_t to R.string.g6_b,
    R.string.g7_t to R.string.g7_b, R.string.g8_t to R.string.g8_b,
)

@Composable
fun GuideScreen(vm: AppViewModel, first: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val speech by vm.speech.state.collectAsState()
    val paragraphs = SECTIONS.map { (t, b) -> ctx.getString(t) + ". " + ctx.getString(b) }

    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.guide_title), onBack = if (first) null else onClose)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeechBar(vm, paragraphs)
                SECTIONS.forEachIndexed { i, (t, b) ->
                    val active = speech.playing && speech.index == i
                    SmartCard(
                        modifier = if (active) Modifier.border(2.dp, AppTheme.colors.gold, RoundedCornerShape(20.dp)) else Modifier,
                    ) {
                        Text(stringResource(t), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold)
                        Text(
                            stringResource(b), modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (first) {
                GoldButton(
                    stringResource(R.string.guide_start),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onClick = onClose,
                )
            }
        }
    }
}
