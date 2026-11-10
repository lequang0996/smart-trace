package com.fws.smarttrace.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.fws.smarttrace.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference

/**
 * Google AdMob App Open ad.
 *
 * - Shown right after a cold start as soon as the ad is ready (only within the first few seconds, so it never
 *   pops up in the middle of something).
 * - Shown again when the person comes back to the app after leaving it, at most once every [MIN_GAP_MS].
 * - Never shown before onboarding is finished, and never when returning from the camera / gallery picker.
 * Everything runs on the main thread. Ads are only requested once [AdsManager] says consent allows it.
 */
object AppOpenAds {
    private const val MAX_AGE_MS = 4 * 60 * 60 * 1000L   // an App Open ad expires after 4 hours
    private const val COLD_START_WINDOW_MS = 10_000L
    private const val MIN_GAP_MS = 2 * 60 * 1000L

    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var showing = false
    private var lastShownAt = 0L
    private var coldStart = true
    private var skipNext = false
    private var enabled = false
    private var current: WeakReference<Activity>? = null
    private val processStartedAt = SystemClock.elapsedRealtime()

    private fun available(): Boolean =
        ad != null && SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MS

    /** Call before sending the person to another app (camera, picker) so coming back does not trigger an ad. */
    fun suppressNext() { skipNext = true }

    /** Activity became visible. [adsAllowed] is false until onboarding is finished. */
    fun onStart(activity: Activity, adsAllowed: Boolean) {
        current = WeakReference(activity)
        enabled = adsAllowed
        if (!enabled) return
        if (skipNext) { skipNext = false; return }
        if (coldStart && SystemClock.elapsedRealtime() - processStartedAt > COLD_START_WINDOW_MS) coldStart = false
        if (coldStart) {
            if (available()) show(activity) else load(activity)   // otherwise shown when the load finishes
            return
        }
        if (SystemClock.elapsedRealtime() - lastShownAt >= MIN_GAP_MS) {
            if (available()) show(activity) else load(activity)
        }
    }

    fun onStop(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    /** Called by [AdsManager] when the Mobile Ads SDK is ready, and after each ad has been shown. */
    fun load(context: Context) {
        if (loading || available() || !AdsManager.ready.value) return
        loading = true
        AppOpenAd.load(
            context.applicationContext, BuildConfig.ADMOB_APP_OPEN_ID, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    loading = false
                    ad = loaded
                    loadedAt = SystemClock.elapsedRealtime()
                    val a = current?.get()
                    val inWindow = SystemClock.elapsedRealtime() - processStartedAt <= COLD_START_WINDOW_MS
                    if (a != null && enabled && coldStart && inWindow) show(a)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    private fun show(activity: Activity) {
        val toShow = ad ?: return
        if (showing || activity.isFinishing || activity.isDestroyed) return
        toShow.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finished(activity)
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finished(activity)
        }
        showing = true
        coldStart = false
        lastShownAt = SystemClock.elapsedRealtime()
        toShow.show(activity)
    }

    private fun finished(context: Context) {
        showing = false
        ad = null
        load(context)
    }
}
