package com.allensandiego.notepad

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.performance.BugsnagPerformance
import com.google.android.gms.ads.MobileAds

class NotepadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Bugsnag.start(this)
        BugsnagPerformance.start(this)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
    }
}