package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.BatonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatonManagerTest {

    private val manager = BatonManager(defaultTurnSeconds = 30, queuePosition = 1)

    @Test
    fun `requestBaton from Idle moves to InQueue`() {
        val state = manager.requestBaton()
        assertTrue(state is BatonState.InQueue)
        state as BatonState.InQueue
        assertEquals(1, state.position)
        assertEquals(30, state.estimatedWaitSeconds)
    }

    @Test
    fun `grantBaton only applies while InQueue`() {
        manager.requestBaton()
        val state = manager.grantBaton(timeSeconds = 15)
        assertTrue(state is BatonState.HoldingBaton)
        assertEquals(15, (state as BatonState.HoldingBaton).timeRemainingSeconds)
    }

    @Test
    fun `grantBaton is ignored when Idle`() {
        val state = manager.grantBaton(timeSeconds = 15)
        assertTrue(state is BatonState.Idle)
    }

    @Test
    fun `tick counts down HoldingBaton and ends at Idle`() {
        manager.requestBaton()
        manager.grantBaton(timeSeconds = 2)

        val first = manager.tick()
        assertEquals(1, (first as BatonState.HoldingBaton).timeRemainingSeconds)

        val second = manager.tick()
        assertTrue(second is BatonState.Idle)
    }

    @Test
    fun `passBaton while holding returns floor`() {
        manager.requestBaton()
        manager.grantBaton(timeSeconds = 30)
        val state = manager.passBaton()
        assertTrue(state is BatonState.Idle)
    }

    @Test
    fun `leaveQueue while queued returns to Idle`() {
        manager.requestBaton()
        val state = manager.leaveQueue()
        assertTrue(state is BatonState.Idle)
    }

    @Test
    fun `requestBaton while holding does not requeue`() {
        manager.requestBaton()
        manager.grantBaton(timeSeconds = 30)
        val state = manager.requestBaton()
        assertTrue(state is BatonState.HoldingBaton)
    }

    @Test
    fun `Listening state can request the baton`() {
        val listeningManager = BatonManager()
        // simulate floor held by someone else, then returned
        listeningManager.requestBaton()
        listeningManager.passBaton()
        val state = listeningManager.requestBaton()
        assertTrue(state is BatonState.InQueue)
    }
}
