package com.mutepoint.app.core.automation.engine

import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.model.WorkflowContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowEngineTest {

    private val engine = WorkflowEngine(ConditionEvaluator())

    @Test
    fun disabledWorkflow_skipped() = runBlocking {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            enabled = false,
            trigger = Trigger.Manual,
            actions = listOf(Action.StartMonitoring),
        )
        val executed = engine.execute(workflow, WorkflowContext()) {}
        assertFalse(executed)
    }

    @Test
    fun conditionsNotMet_skipped() = runBlocking {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            enabled = true,
            trigger = Trigger.Manual,
            conditions = listOf(Condition.InterruptionCountAbove(5)),
            actions = listOf(Action.StartMonitoring),
        )
        val ctx = WorkflowContext(interruptionCount = 2)
        val executed = engine.execute(workflow, ctx) {}
        assertFalse(executed)
    }

    @Test
    fun conditionsMet_executed() = runBlocking {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            enabled = true,
            trigger = Trigger.Manual,
            conditions = listOf(Condition.InterruptionCountAbove(3)),
            actions = listOf(Action.StartMonitoring),
        )
        val ctx = WorkflowContext(interruptionCount = 5)
        val executed = engine.execute(workflow, ctx) {}
        assertTrue(executed)
    }

    @Test
    fun noConditions_executed() = runBlocking {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            enabled = true,
            trigger = Trigger.Manual,
            actions = listOf(Action.StartMonitoring),
        )
        val executed = engine.execute(workflow, WorkflowContext()) {}
        assertTrue(executed)
    }

    @Test
    fun actionsExecuted_inOrder() = runBlocking {
        val executedActions = mutableListOf<Action>()
        val workflow = Workflow(
            id = "1",
            name = "Test",
            enabled = true,
            trigger = Trigger.Manual,
            actions = listOf(
                Action.StartMonitoring,
                Action.ShowNotification("Title", "Body"),
                Action.StopMonitoring,
            ),
        )
        engine.execute(workflow, WorkflowContext()) { action ->
            executedActions.add(action)
        }
        assertTrue(executedActions.size == 3)
        assertTrue(executedActions[0] is Action.StartMonitoring)
        assertTrue(executedActions[1] is Action.ShowNotification)
        assertTrue(executedActions[2] is Action.StopMonitoring)
    }

    @Test
    fun matchesTrigger_onEvent() {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            trigger = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED),
        )
        val event = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED)
        assertTrue(engine.matchesTrigger(workflow, event))
    }

    @Test
    fun matchesTrigger_wrongEvent() {
        val workflow = Workflow(
            id = "1",
            name = "Test",
            trigger = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED),
        )
        val event = Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_LEFT)
        assertFalse(engine.matchesTrigger(workflow, event))
    }
}
