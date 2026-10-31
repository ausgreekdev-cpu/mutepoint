package com.mutepoint.app.core.automation.model

import kotlinx.serialization.Serializable

/**
 * An action defines what a workflow does when triggered.
 */
@Serializable
sealed class Action {
    /** Start audio monitoring. */
    @Serializable
    data object StartMonitoring : Action()

    /** Stop audio monitoring. */
    @Serializable
    data object StopMonitoring : Action()

    /** Create a journal entry. */
    @Serializable
    data class SaveJournal(
        val trigger: String,
        val reality: String,
    ) : Action()

    /** Show a local notification. */
    @Serializable
    data class ShowNotification(
        val title: String,
        val body: String,
    ) : Action()

    /** Navigate to a screen. */
    @Serializable
    data class NavigateTo(val screen: String) : Action()

    /** Run another workflow by ID. */
    @Serializable
    data class RunWorkflow(val workflowId: String) : Action()
}
