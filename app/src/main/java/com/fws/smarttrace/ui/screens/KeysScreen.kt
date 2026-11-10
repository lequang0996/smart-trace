package com.fws.smarttrace.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.R
import com.fws.smarttrace.core.SecureKeyStore
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.components.GhostButton
import com.fws.smarttrace.ui.components.GoldButton
import com.fws.smarttrace.ui.components.IconAction
import com.fws.smarttrace.ui.components.ScreenSurface
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.components.TopBar
import com.fws.smarttrace.ui.theme.AppTheme

@Composable
fun KeysScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val st by vm.settings.collectAsState()
    val saved by vm.keysSaved.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(saved) { if (saved != null) { kotlinx.coroutines.delay(2500); vm.keysSavedShown() } }

    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.keys_title), onBack = onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SmartCard {
                    Text(stringResource(R.string.keys_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        minLines = 3, maxLines = 6,
                        placeholder = { Text(stringResource(R.string.keys_input_hint)) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    )
                    GoldButton(
                        stringResource(R.string.keys_save), modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        icon = R.drawable.ic_key, enabled = SecureKeyStore.parseLines(text).isNotEmpty(),
                    ) {
                        vm.addKeys(text)
                        text = ""
                    }
                    saved?.let {
                        Text(
                            stringResource(R.string.keys_added, it.toString()), modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.success,
                        )
                    }
                }

                SmartCard {
                    if (st.keys.isEmpty()) {
                        Text(stringResource(R.string.keys_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        st.keys.forEach { k ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    SecureKeyStore.mask(k), modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                IconAction(R.drawable.ic_trash, tint = MaterialTheme.colorScheme.onSurfaceVariant) { vm.removeKey(k) }
                            }
                        }
                    }
                }

                SmartCard {
                    Text(stringResource(R.string.keys_howto_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    val steps = listOf(R.string.keys_s1, R.string.keys_s2, R.string.keys_s3, R.string.keys_s5, R.string.keys_s6)
                    steps.forEachIndexed { i, res ->
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                (i + 1).toString(), modifier = Modifier.width(26.dp),
                                style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.gold,
                            )
                            Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    GhostButton(
                        stringResource(R.string.keys_open_studio), modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        icon = R.drawable.ic_link,
                    ) {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
                        } catch (e: ActivityNotFoundException) { }
                    }
                    Text(
                        stringResource(R.string.keys_free_note), modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
