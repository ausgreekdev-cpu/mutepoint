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
 *
 * Each action type maps to a specific controller or repository call.
 * Navigation actions use [NavigationBus] for cross-screen communication.
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
                NavigationBus.navigateTo(action.screen)
            }
            is Action.RunWorkflow -> {
                val workflow = workflowRepository.getWorkflow(action.workflowId)
                if (workflow != null) {
                    val depth = com.mutepoint.app.core.automation.model.WorkflowContext().get<Int>("depth") ?: 0
                    if (depth >= 3) {
                        android.util.Log.w("ActionHandler", "Max workflow recursion depth reached")
                        return@execute
                    }
                    val ctx = com.mutepoint.app.core.automation.model.WorkflowContext().apply {
                        set("depth", depth + 1)
                    }
                    workflowEngine.execute(workflow, ctx) { nestedAction ->
                        if (nestedAction is Action.RunWorkflow) {
                            // Recursive call with depth tracking
                            val nestedWorkflow = workflowRepository.getWorkflow(nestedAction.workflowId)
                            if (nestedWorkflow != null && depth + 1 < 3) {
                                val nestedCtx = com.mutepoint.app.core.automation.model.WorkflowContext().apply {
                                    set("depth", depth + 2)
                                }
                                workflowEngine.execute(nestedWorkflow, nestedCtx) { deeper -> execute(deeper) }
                            }
                        } else {
                            execute(nestedAction)
                        }
                    }
                }
            }
        }
    }
}
