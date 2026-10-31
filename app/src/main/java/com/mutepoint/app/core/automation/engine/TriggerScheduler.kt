package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules time-based workflow triggers using WorkManager.
 */
@Singleton
class TriggerScheduler @Inject constructor(
    private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a workflow's trigger if it's time-based.
     */
    fun schedule(workflow: Workflow) {
        val trigger = workflow.trigger as? Trigger.OnSchedule ?: return
        val schedule = trigger.schedule

        val initialDelay = calculateInitialDelay(schedule)
        val request = PeriodicWorkRequestBuilder<WorkflowWorker>(
            if (schedule.repeat) 24 else Long.MAX_VALUE, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("workflow_${workflow.id}")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "workflow_${workflow.id}",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Cancel a scheduled workflow.
     */
    fun cancel(workflowId: String) {
        workManager.cancelUniqueWork("workflow_$workflowId")
    }

    private fun calculateInitialDelay(schedule: com.mutepoint.app.core.automation.model.Schedule): Long {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        cal.set(java.util.Calendar.HOUR_OF_DAY, schedule.hour)
        cal.set(java.util.Calendar.MINUTE, schedule.minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        if (cal.timeInMillis <= now) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        // Adjust to next valid day of week
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) !in schedule.daysOfWeek.map { it % 7 + 1 }) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis - now
    }
}
