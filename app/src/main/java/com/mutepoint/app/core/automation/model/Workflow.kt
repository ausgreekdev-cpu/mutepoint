package com.mutepoint.app.core.automation.model

import kotlinx.serialization.Serializable

/**
 * A workflow definition: trigger + conditions + actions.
 */
@Serializable
data class Workflow(
    val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
