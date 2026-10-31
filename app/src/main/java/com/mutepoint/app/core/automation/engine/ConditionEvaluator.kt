package com.mutepoint.app.core.automation.engine

import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.core.automation.model.WorkflowContext
import java.util.Calendar

/**
 * Evaluates whether conditions are met given the current workflow context.
 */
class ConditionEvaluator {

    fun evaluateAll(conditions: List<Condition>, context: WorkflowContext): Boolean =
        conditions.all { evaluate(it, context) }

    fun evaluate(condition: Condition, context: WorkflowContext): Boolean = when (condition) {
        is Condition.InterruptionCountAbove -> context.interruptionCount > condition.threshold
        is Condition.SpeakingTimeBelow -> context.speakingTimeMs < condition.seconds * 1000L
        is Condition.TimeOfDay -> {
            val cal = Calendar.getInstance().apply { timeInMillis = context.timestamp }
            cal.get(Calendar.HOUR_OF_DAY) == condition.hour &&
                cal.get(Calendar.MINUTE) == condition.minute
        }
        is Condition.DayOfWeek -> {
            val cal = Calendar.getInstance().apply { timeInMillis = context.timestamp }
            val day = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday
            // Convert to 1=Monday..7=Sunday
            val mondayBased = if (day == Calendar.SUNDAY) 7 else day - 1
            mondayBased in condition.days
        }
        is Condition.HasInterruptions -> context.interruptionCount > 0
        is Condition.UserSpokeMore -> context.speakingTimeMs > context.otherSpeakingTimeMs
    }
}
