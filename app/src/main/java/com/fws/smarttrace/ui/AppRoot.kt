package com.fws.smarttrace.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fws.smarttrace.R
import com.fws.smarttrace.ui.components.BannerAd
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.screens.GuideScreen
import com.fws.smarttrace.ui.screens.HistoryScreen
import com.fws.smarttrace.ui.screens.KeysScreen
import com.fws.smarttrace.ui.screens.ResultScreen
import com.fws.smarttrace.ui.screens.ScanScreen
import com.fws.smarttrace.ui.screens.SettingsScreen
import com.fws.smarttrace.ui.theme.AppTheme

private const val OVERLAY_NONE = ""
private const val OVERLAY_GUIDE = "guide"
private const val OVERLAY_KEYS = "keys"

private data class NavItem(val label: Int, val icon: Int)

@Composable
fun AppRoot(
    vm: AppViewModel,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    updateReady: Boolean,
    onRestart: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val opened by vm.opened.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf(OVERLAY_NONE) }
    val snackbar = remember { SnackbarHostState() }

    val updateMsg = stringResource(R.string.update_ready)
    val restartLabel = stringResource(R.string.update_restart)
    LaunchedEffect(updateReady) {
        if (updateReady) {
            val r = snackbar.showSnackbar(updateMsg, restartLabel, duration = SnackbarDuration.Indefinite)
            if (r == SnackbarResult.ActionPerformed) onRestart()
        }
    }

    BackHandler(enabled = overlay != OVERLAY_NONE && settings.onboarded) { overlay = OVERLAY_NONE }
    BackHandler(enabled = overlay == OVERLAY_NONE && opened != null) { vm.closeResult() }
    BackHandler(enabled = overlay == OVERLAY_NONE && opened == null && tab != 0) { tab = 0 }

    val result = opened
    when {
        !settings.onboarded -> GuideScreen(vm, first = true, onClose = { vm.finishOnboarding() })
        overlay == OVERLAY_KEYS -> KeysScreen(vm, onBack = { overlay = OVERLAY_NONE })
        overlay == OVERLAY_GUIDE -> GuideScreen(vm, first = false, onClose = { overlay = OVERLAY_NONE })
        result != null -> ResultScreen(vm, result, onBack = { vm.closeResult() })
        else -> {
            val items = listOf(
                NavItem(R.string.nav_scan, R.drawable.ic_scan_code),
                NavItem(R.string.nav_history, R.drawable.ic_history),
                NavItem(R.string.nav_favorites, R.drawable.ic_heart),
                NavItem(R.string.nav_settings, R.drawable.ic_sliders),
            )
            val c = AppTheme.colors
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        items.forEachIndexed { i, it ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Ico(it.icon) },
                                label = { Text(stringResource(it.label), maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.gold,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = c.gold.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> ScanScreen(vm, onCamera, onGallery, onOpenKeys = { overlay = OVERLAY_KEYS })
                            1 -> HistoryScreen(vm, favoritesOnly = false)
                            2 -> HistoryScreen(vm, favoritesOnly = true)
                            else -> SettingsScreen(vm, onGuide = { overlay = OVERLAY_GUIDE }, onKeys = { overlay = OVERLAY_KEYS })
                        }
                    }
                    // Visible as soon as the app opens (History / Settings carry their own in-list banner).
                    if (tab == 0) BannerAd(verticalPadding = 6.dp)
                }
            }
        }
    }
}
