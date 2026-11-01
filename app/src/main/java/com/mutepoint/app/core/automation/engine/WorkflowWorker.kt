package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mutepoint.app.core.automation.model.WorkflowContext
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WorkflowWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workflowRepository: WorkflowRepository,
    private val workflowEngine: WorkflowEngine,
    private val actionHandler: ActionHandler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val workflowId = inputData.getString("workflowId") ?: return Result.failure()
        val workflow = workflowRepository.getWorkflow(workflowId) ?: return Result.failure()
        return try {
            val executed = workflowEngine.execute(workflow, WorkflowContext()) { action ->
                actionHandler.execute(action)
            }
            if (executed) Result.success() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}