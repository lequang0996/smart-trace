package com.fws.smarttrace.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob + User Messaging Platform (UMP). Ads are only initialised once the consent flow
 * says ads can be requested (GDPR / EEA / UK / US-state rules are handled by the UMP form).
 */
object AdsManager {
    private val initialized = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun start(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        if (info.canRequestAds()) initAds(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refresh(activity, info)
                }
            },
            { refresh(activity, info) }
        )
    }

    private fun refresh(activity: Activity, info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) initAds(activity)
    }

    private fun initAds(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread {
            MobileAds.initialize(app) {
                _ready.value = true
                AppOpenAds.load(app)
            }
        }.start()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}
