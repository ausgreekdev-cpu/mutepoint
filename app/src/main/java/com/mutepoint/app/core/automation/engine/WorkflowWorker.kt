package com.mutepoint.app.core.automation.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.WorkflowContext
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WorkflowWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun workflowRepository(): WorkflowRepository
        fun workflowEngine(): WorkflowEngine
        fun actionHandler(): ActionHandler
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java,
            )
            val repository = entryPoint.workflowRepository()
            val engine = entryPoint.workflowEngine()
            val actionHandler = entryPoint.actionHandler()

            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            val dayOfWeek = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // Mon=1..Sun=7

            val workflows = repository.observeWorkflows().first()
            val dueWorkflows = workflows.filter { workflow ->
                if (!workflow.enabled) return@filter false
                val trigger = workflow.trigger
                if (trigger !is Trigger.OnSchedule) return@filter false
                val schedule = trigger.schedule
                schedule.daysOfWeek.contains(dayOfWeek) &&
                    kotlin.math.abs(schedule.hour * 60 + schedule.minute - (hour * 60 + minute)) <= 15
            }

            for (workflow in dueWorkflows) {
                engine.execute(workflow, WorkflowContext()) { action ->
                    actionHandler.execute(action)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
