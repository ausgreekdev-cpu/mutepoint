package com.mutepoint.app.core.automation.engine

import android.util.Log
import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.model.WorkflowContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes workflows: evaluates conditions and runs actions in order.
 */
@Singleton
class WorkflowEngine @Inject constructor(
    private val conditionEvaluator: ConditionEvaluator,
) {
    companion object {
        private const val TAG = "WorkflowEngine"
    }

    /**
     * Execute a workflow if conditions are met.
     * @return true if the workflow was executed, false if skipped.
     */
    suspend fun execute(
        workflow: Workflow,
        context: WorkflowContext,
        actionHandler: suspend (Action) -> Unit,
    ): Boolean {
        if (!workflow.enabled) {
            Log.d(TAG, "Workflow '${workflow.name}' is disabled, skipping")
            return false
        }

        if (!conditionEvaluator.evaluateAll(workflow.conditions, context)) {
            Log.d(TAG, "Workflow '${workflow.name}' conditions not met, skipping")
            return false
        }

        Log.d(TAG, "Executing workflow '${workflow.name}'")
        for (action in workflow.actions) {
            try {
                actionHandler(action)
            } catch (e: Exception) {
                Log.e(TAG, "Action failed in '${workflow.name}': ${e.message}", e)
            }
        }
        return true
    }

    /**
     * Check if a workflow's trigger matches the given event type.
     */
    fun matchesTrigger(workflow: Workflow, eventType: Trigger.OnEvent? = null): Boolean {
        return when (workflow.trigger) {
            is Trigger.Manual -> eventType == null
            is Trigger.OnEvent -> eventType != null && workflow.trigger.eventType == eventType.eventType
            is Trigger.OnSchedule -> false // handled by scheduler
            is Trigger.OnCondition -> conditionEvaluator.evaluateAll(workflow.trigger.conditions, WorkflowContext())
        }
    }
}
