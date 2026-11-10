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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fws.smarttrace.BuildConfig
import com.fws.smarttrace.ads.AdsManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive banner, shown only after the consent flow allows ads. It sits in its own row (the scan screen pins it
 * above the bottom navigation, history / settings place it inside the list) and never on the result screen.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier, verticalPadding: Dp = 20.dp) {
    val ready by AdsManager.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val widthDp = (with(density) { windowWidthPx.toDp().value }.toInt() - 32).coerceAtLeast(320)
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
    Box(modifier.fillMaxWidth().padding(vertical = verticalPadding), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView })
    }
}
