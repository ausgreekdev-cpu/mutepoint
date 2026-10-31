package com.mutepoint.app.core.automation.model

import kotlinx.serialization.Serializable

/**
 * A trigger defines when a workflow should execute.
 */
@Serializable
sealed class Trigger {
    /** User manually taps a button to run the workflow. */
    @Serializable
    data object Manual : Trigger()

    /** Fires when a specific app event occurs. */
    @Serializable
    data class OnEvent(val eventType: EventType) : Trigger()

    /** Fires on a schedule (cron-like). */
    @Serializable
    data class OnSchedule(val schedule: Schedule) : Trigger()

    /** Fires when all conditions evaluate to true. */
    @Serializable
    data class OnCondition(val conditions: List<Condition>) : Trigger()
}

@Serializable
enum class EventType {
    ROOM_JOINED,
    ROOM_LEFT,
    INTERRUPTION_DETECTED,
    SESSION_ENDED,
    JOURNAL_ENTRY_CREATED,
    APP_OPENED,
}

@Serializable
data class Schedule(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1=Monday, 7=Sunday
    val repeat: Boolean = true,
)
