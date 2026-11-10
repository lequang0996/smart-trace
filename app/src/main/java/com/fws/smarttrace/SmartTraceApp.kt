package com.fws.smarttrace

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.core.Prefs

class SmartTraceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs(this).nightMode)
        Languages.ensureDefault()
    }
}
