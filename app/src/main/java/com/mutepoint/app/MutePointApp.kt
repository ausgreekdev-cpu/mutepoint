package com.mutepoint.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MutePointApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching { System.loadLibrary("sqlcipher") }
    }
}
