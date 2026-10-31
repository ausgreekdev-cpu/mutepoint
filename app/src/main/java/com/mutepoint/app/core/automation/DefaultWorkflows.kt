package com.mutepoint.app.core.automation

import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates default workflows on first app launch.
 */
@Singleton
class DefaultWorkflows @Inject constructor(
    private val workflowRepository: WorkflowRepository,
) {
    fun createDefaultsIfNeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val existing = workflowRepository.observeWorkflows()
            // Check if any workflows exist — if not, create defaults
            // (In production, you'd check a flag in SharedPreferences)
        }
    }

    suspend fun createDefaults() {
        val defaults = listOf(
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Log Interruptions",
                description = "Auto-create a journal entry when a session has interruptions",
                trigger = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.SESSION_ENDED),
                conditions = listOf(Condition.HasInterruptions),
                actions = listOf(
                    Action.SaveJournal(
                        trigger = "Session had interruptions",
                        reality = "I practiced assertive communication",
                    ),
                ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Practice Reminder",
                description = "Daily reminder to practice assertive communication",
                trigger = Trigger.OnSchedule(
                    com.mutepoint.app.core.automation.model.Schedule(
                        hour = 9,
                        minute = 0,
                        daysOfWeek = setOf(1, 2, 3, 4, 5),
                        repeat = true,
                    )
                ),
                actions = listOf(
                    Action.ShowNotification(
                        title = "Practice Time",
                        body = "Take a moment to practice assertive communication today.",
                    ),
                ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Safe Room Auto-Monitor",
                description = "Start monitoring automatically when joining a Safe Room",
                trigger = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED),
                actions = listOf(Action.StartMonitoring),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Session Summary",
                description = "Show a summary notification after each session",
                trigger = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.SESSION_ENDED),
                actions = listOf(
                    Action.ShowNotification(
                        title = "Session Complete",
                        body = "Your session has been recorded. Check Insights for details.",
                    ),
                ),
            ),
        )

        for (workflow in defaults) {
            workflowRepository.saveWorkflow(workflow)
        }
    }
}
