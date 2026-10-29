package com.mutepoint.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mutepoint.app.core.service.MonitorEntryPoint
import com.mutepoint.app.core.service.MonitorService
import dagger.hilt.android.EntryPointAccessors

/**
 * Debug-only bridge: forwards SIMULATE_INTERRUPTION broadcasts to the running
 * MonitorService. Guarded by the DUMP permission (adb shell only) and compiled
 * only into the debug source set, so release builds have no such surface.
 */
class DebugSimulateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MonitorService.ACTION_SIMULATE_INTERRUPTION) return

        val controller = EntryPointAccessors
            .fromApplication(context.applicationContext, MonitorEntryPoint::class.java)
            .monitorController()
        if (!controller.isMonitoring.value) return

        val forward = Intent(context, MonitorService::class.java).apply {
            action = MonitorService.ACTION_SIMULATE_INTERRUPTION
            intent.extras?.let { putExtras(it) }
        }
        context.startService(forward)
    }
}
