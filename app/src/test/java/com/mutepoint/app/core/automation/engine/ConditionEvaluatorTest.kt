package com.mutepoint.app.core.automation.engine

import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.core.automation.model.WorkflowContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionEvaluatorTest {

    private val evaluator = ConditionEvaluator()

    @Test
    fun interruptionCountAbove_true() {
        val ctx = WorkflowContext(interruptionCount = 5)
        assertTrue(evaluator.evaluate(Condition.InterruptionCountAbove(3), ctx))
    }

    @Test
    fun interruptionCountAbove_false() {
        val ctx = WorkflowContext(interruptionCount = 2)
        assertFalse(evaluator.evaluate(Condition.InterruptionCountAbove(3), ctx))
    }

    @Test
    fun speakingTimeBelow_true() {
        val ctx = WorkflowContext(speakingTimeMs = 30_000)
        assertTrue(evaluator.evaluate(Condition.SpeakingTimeBelow(60), ctx))
    }

    @Test
    fun speakingTimeBelow_false() {
        val ctx = WorkflowContext(speakingTimeMs = 90_000)
        assertFalse(evaluator.evaluate(Condition.SpeakingTimeBelow(60), ctx))
    }

    @Test
    fun hasInterruptions_true() {
        val ctx = WorkflowContext(interruptionCount = 1)
        assertTrue(evaluator.evaluate(Condition.HasInterruptions, ctx))
    }

    @Test
    fun hasInterruptions_false() {
        val ctx = WorkflowContext(interruptionCount = 0)
        assertFalse(evaluator.evaluate(Condition.HasInterruptions, ctx))
    }

    @Test
    fun userSpokeMore_true() {
        val ctx = WorkflowContext(speakingTimeMs = 60_000, otherSpeakingTimeMs = 30_000)
        assertTrue(evaluator.evaluate(Condition.UserSpokeMore, ctx))
    }

    @Test
    fun userSpokeMore_false() {
        val ctx = WorkflowContext(speakingTimeMs = 20_000, otherSpeakingTimeMs = 50_000)
        assertFalse(evaluator.evaluate(Condition.UserSpokeMore, ctx))
    }

    @Test
    fun evaluateAll_allTrue() {
        val ctx = WorkflowContext(interruptionCount = 5, speakingTimeMs = 30_000)
        val conditions = listOf(
            Condition.InterruptionCountAbove(3),
            Condition.SpeakingTimeBelow(60),
        )
        assertTrue(evaluator.evaluateAll(conditions, ctx))
    }

    @Test
    fun evaluateAll_oneFalse() {
        val ctx = WorkflowContext(interruptionCount = 5, speakingTimeMs = 90_000)
        val conditions = listOf(
            Condition.InterruptionCountAbove(3),
            Condition.SpeakingTimeBelow(60),
        )
        assertFalse(evaluator.evaluateAll(conditions, ctx))
    }

    @Test
    fun evaluateAll_empty() {
        val ctx = WorkflowContext()
        assertTrue(evaluator.evaluateAll(emptyList(), ctx))
    }
}
