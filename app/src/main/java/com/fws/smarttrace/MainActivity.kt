package com.fws.smarttrace

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import com.fws.smarttrace.ads.AdsManager
import com.fws.smarttrace.ads.AppOpenAds
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.core.Prefs
import com.fws.smarttrace.ui.AppRoot
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.theme.SmartTraceTheme
import java.io.File

class MainActivity : AppCompatActivity() {
    private val vm: AppViewModel by viewModels()
    private lateinit var update: InAppUpdate
    private var updateReady by mutableStateOf(false)

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val prefs = Prefs(this)
        val path = prefs.pendingCameraPath
        prefs.pendingCameraPath = null
        if (ok && path != null) {
            vm.analyze(FileProvider.getUriForFile(this, "$packageName.fileprovider", File(path)))
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyze(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must run once an AppCompat activity exists: from Application.onCreate it silently does nothing on Android 13+.
        Languages.ensureDefault()
        enableEdgeToEdge()
        update = InAppUpdate(this) { updateReady = true }
        AdsManager.start(this)
        if (savedInstanceState == null) handleShare(intent)

        setContent {
            SmartTraceTheme {
                AppRoot(
                    vm = vm,
                    onCamera = ::launchCamera,
                    onGallery = {
                        AppOpenAds.suppressNext()
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    updateReady = updateReady,
                    onRestart = { update.completeUpdate() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    override fun onStart() {
        super.onStart()
        AppOpenAds.onStart(this, Prefs(this).onboarded)
    }

    override fun onStop() {
        AppOpenAds.onStop(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        update.check()
    }

    override fun onDestroy() {
        update.release()
        super.onDestroy()
    }

    /** Lets people share a picture from the gallery straight into SmartTrace. */
    private fun handleShare(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND && i.type?.startsWith("image/") == true) {
            IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)?.let { vm.analyze(it) }
        }
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3_600_000L) it.delete() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        Prefs(this).pendingCameraPath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        try {
            AppOpenAds.suppressNext()
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.err_generic, "camera"), Toast.LENGTH_SHORT).show()
        }
    }
}
