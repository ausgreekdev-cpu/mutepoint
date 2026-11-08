package com.mutepoint.app.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val ACTION_CHECK_IN_DUE = "com.mutepoint.app.action.CHECK_IN_DUE"
        private const val REQUEST_CODE = 2001
        const val PREFS = "checkin_prefs"
        private const val KEY_DUE_TIME = "due_time"
        const val KEY_ACTIVE = "active"
    }

    fun scheduleCheckIn(minutes: Int): Long {
        val dueTime = System.currentTimeMillis() + minutes * 60_000L
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putLong(KEY_DUE_TIME, dueTime).putBoolean(KEY_ACTIVE, true) }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, CheckInReceiver::class.java).apply {
            action = ACTION_CHECK_IN_DUE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (alarmManager != null) {
            val triggerAt = SystemClock.elapsedRealtime() + minutes * 60_000L
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
        }
        return dueTime
    }

    fun cancelCheckIn() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_ACTIVE, false).remove(KEY_DUE_TIME) }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, CheckInReceiver::class.java).apply {
            action = ACTION_CHECK_IN_DUE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pendingIntent)
    }

    fun isActive(): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACTIVE, false)
    }

    fun getDueTime(): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_DUE_TIME, 0L)
    }
}

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != CheckInManager.ACTION_CHECK_IN_DUE) return
        // For now, open the app's SOS screen as a prompt - in production this could send alerts
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_sos", true)
        }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
        val prefs = context.getSharedPreferences(CheckInManager.PREFS, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(CheckInManager.KEY_ACTIVE, false) }
    }
}
