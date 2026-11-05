package com.mutepoint.app.core.automation.engine

import com.mutepoint.app.core.automation.model.AppEvent
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.model.WorkflowContext
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for app events and executes matching workflows.
 */
@Singleton
class WorkflowTriggerListener @Inject constructor(
    private val appEventBus: AppEventBus,
    private val workflowRepository: WorkflowRepository,
    private val workflowEngine: WorkflowEngine,
    private val actionHandler: ActionHandler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            appEventBus.events.collectLatest { event ->
                handleEvent(event)
            }
        }
    }

    private suspend fun handleEvent(event: AppEvent) {
        val eventType = when (event) {
            is AppEvent.RoomJoined -> com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED
            is AppEvent.RoomLeft -> com.mutepoint.app.core.automation.model.EventType.ROOM_LEFT
            is AppEvent.InterruptionDetected -> com.mutepoint.app.core.automation.model.EventType.INTERRUPTION_DETECTED
            is AppEvent.SessionEnded -> com.mutepoint.app.core.automation.model.EventType.SESSION_ENDED
            is AppEvent.JournalEntryCreated -> com.mutepoint.app.core.automation.model.EventType.JOURNAL_ENTRY_CREATED
            is AppEvent.AppOpened -> com.mutepoint.app.core.automation.model.EventType.APP_OPENED
        }

        val workflows = workflowRepository.observeWorkflows().first()
        val matchingWorkflows = workflows.filter { workflow ->
            workflow.enabled && workflow.trigger is Trigger.OnEvent &&
                (workflow.trigger as Trigger.OnEvent).eventType == eventType
        }

        val context = when (event) {
            is AppEvent.InterruptionDetected -> WorkflowContext(interruptionCount = 1)
            is AppEvent.SessionEnded -> WorkflowContext(
                sessionId = event.sessionId,
                speakingTimeMs = event.speakingMs,
                interruptionCount = event.interruptionCount,
            )
            is AppEvent.RoomJoined -> WorkflowContext(roomName = event.roomName)
            is AppEvent.RoomLeft -> WorkflowContext(roomName = event.roomName)
            is AppEvent.JournalEntryCreated -> WorkflowContext()
            is AppEvent.AppOpened -> WorkflowContext()
        }

        for (workflow in matchingWorkflows) {
            workflowEngine.execute(workflow, context) { action ->
                actionHandler.execute(action)
            }
        }
    }
}
