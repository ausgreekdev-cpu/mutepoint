package com.mutepoint.app.core.automation.model

/**
 * Runtime context passed to the workflow engine for condition evaluation.
 */
data class WorkflowContext(
    val interruptionCount: Int = 0,
    val speakingTimeMs: Long = 0,
    val otherSpeakingTimeMs: Long = 0,
    val sessionId: String? = null,
    val roomName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
