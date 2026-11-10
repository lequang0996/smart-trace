package com.fws.smarttrace.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fws.smarttrace.BuildConfig
import com.fws.smarttrace.ads.AdsManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive banner. Placed inside scrolling content with generous spacing, never next to buttons,
 * and never on the scan / result screens. Shown only after the consent flow allows ads.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    val widthDp = (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(320)
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    Box(modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView })
    }
}
