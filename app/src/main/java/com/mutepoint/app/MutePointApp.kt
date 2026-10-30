package com.mutepoint.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid

@HiltAndroidApp
class MutePointApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching { System.loadLibrary("sqlcipher") }

        if (!BuildConfig.DEBUG) {
            SentryAndroid.init(this) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.isEnableAutoSessionTracking = true
                options.tracesSampleRate = 0.2
            }
        }
    }
}