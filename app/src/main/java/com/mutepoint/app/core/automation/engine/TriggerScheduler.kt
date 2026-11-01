package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(workflow: Workflow) {
        val schedule = (workflow.trigger as? Trigger.OnSchedule)?.schedule ?: return
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now) || equals(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val delay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<WorkflowWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("workflow_${workflow.id}")
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "workflow_${workflow.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(workflowId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("workflow_$workflowId")
    }
}