package com.mutepoint.app.core.automation.model

import kotlinx.serialization.Serializable

/**
 * A condition that must evaluate to true for a workflow to execute.
 */
@Serializable
sealed class Condition {
    /** True if interruption count exceeds threshold. */
    @Serializable
    data class InterruptionCountAbove(val threshold: Int) : Condition()

    /** True if user spoke less than the given seconds. */
    @Serializable
    data class SpeakingTimeBelow(val seconds: Int) : Condition()

    /** True if current time matches hour:minute. */
    @Serializable
    data class TimeOfDay(val hour: Int, val minute: Int) : Condition()

    /** True if today is one of the specified days (1=Mon, 7=Sun). */
    @Serializable
    data class DayOfWeek(val days: Set<Int>) : Condition()

    /** True if the session had any interruptions. */
    @Serializable
    data object HasInterruptions : Condition()

    /** True if user was speaking more than the other person. */
    @Serializable
    data object UserSpokeMore : Condition()
}
