package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that executes scheduled workflows.
 */
class WorkflowWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Workflow execution is handled by the engine when the app is running.
        // For background execution, we'd need to inject the engine via Hilt.
        // For now, this is a placeholder that logs the trigger.
        android.util.Log.d("WorkflowWorker", "Scheduled workflow triggered: ${inputData.getString("workflow_id")}")
        Result.success()
    }
}
