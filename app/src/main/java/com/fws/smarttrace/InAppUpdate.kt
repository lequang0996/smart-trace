package com.fws.smarttrace

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play In-App Updates, FLEXIBLE flow: the update downloads in the background and the user
 * is asked to restart when it is ready. Does nothing when not installed from Google Play.
 * Create it in Activity.onCreate (it registers an activity-result launcher).
 */
class InAppUpdate(
    private val activity: ComponentActivity,
    private val onDownloaded: () -> Unit,
) {
    private val manager = AppUpdateManagerFactory.create(activity)

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) onDownloaded()
    }

    private var listening = false

    fun check() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> onDownloaded()
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        if (!listening) { manager.registerListener(listener); listening = true }
                        runCatching {
                            manager.startUpdateFlowForResult(
                                info, launcher,
                                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { /* not installed from Play, offline, etc. */ }
    }

    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun release() {
        if (listening) { manager.unregisterListener(listener); listening = false }
    }
}
