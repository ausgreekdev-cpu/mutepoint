package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val WORK_NAME = "workflow_schedule_check"
    }

    fun start() {
        val request = PeriodicWorkRequestBuilder<WorkflowWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
