package com.mutepoint.app.core.automation.engine

import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.service.MonitorController
import com.mutepoint.app.domain.model.RealityAnchorEntry
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes workflow actions by delegating to the appropriate controllers/repositories.
 */
interface ActionHandler {
    suspend fun execute(action: Action)
}

@Singleton
class ActionHandlerImpl @Inject constructor(
    private val monitorController: MonitorController,
    private val realityAnchorRepository: RealityAnchorRepository,
    private val workflowRepository: com.mutepoint.app.core.automation.repository.WorkflowRepository,
    private val workflowEngine: WorkflowEngine,
) : ActionHandler {

    override suspend fun execute(action: Action) {
        when (action) {
            is Action.StartMonitoring -> monitorController.start()
            is Action.StopMonitoring -> monitorController.stop()
            is Action.SaveJournal -> {
                realityAnchorRepository.insertEntry(
                    RealityAnchorEntry(
                        triggerText = action.trigger,
                        realityText = action.reality,
                    )
                )
            }
            is Action.ShowNotification -> {
                // Notifications would be posted via NotificationManager
                android.util.Log.d("ActionHandler", "Notification: ${action.title} - ${action.body}")
            }
            is Action.NavigateTo -> {
                // Navigation would be triggered via a shared state or event
                android.util.Log.d("ActionHandler", "Navigate to: ${action.screen}")
            }
            is Action.RunWorkflow -> {
                val workflow = workflowRepository.getWorkflow(action.workflowId)
                if (workflow != null) {
                    workflowEngine.execute(workflow, com.mutepoint.app.core.automation.model.WorkflowContext()) { nestedAction ->
                        execute(nestedAction)
                    }
                }
            }
        }
    }
}
