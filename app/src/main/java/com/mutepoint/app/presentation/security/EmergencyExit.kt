package com.mutepoint.app.presentation.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.mutepoint.app.core.service.MonitorEntryPoint
import dagger.hilt.android.EntryPointAccessors

fun triggerEmergencyExit(activity: Activity) {
    stopMonitoring(activity.applicationContext)
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    activity.startActivity(homeIntent)
    activity.finishAndRemoveTask()
}

private fun stopMonitoring(context: Context) {
    runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            MonitorEntryPoint::class.java,
        )
        entryPoint.monitorController().stop()
    }
}
