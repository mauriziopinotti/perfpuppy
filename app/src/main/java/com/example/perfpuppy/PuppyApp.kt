package com.example.perfpuppy

import android.app.Application
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PuppyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // Crashlytics
        if (!isUnitTest()) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
    }
}

fun isUnitTest(): Boolean {
    return "robolectric" == Build.FINGERPRINT
}
