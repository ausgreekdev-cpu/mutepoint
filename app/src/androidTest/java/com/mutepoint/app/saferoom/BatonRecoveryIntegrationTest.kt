package com.mutepoint.app.saferoom

import com.mutepoint.app.domain.usecase.BatonRequest
import com.mutepoint.app.domain.usecase.BatonSyncReducer
import com.mutepoint.app.domain.usecase.BatonSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Integration tests for baton recovery logic.
 *
 * Verifies that when the current holder disconnects, remaining participants
 * can deterministically compute the next winner from the pending queue,
 * matching the recovery logic in LiveKitRoomManager.
 */
@RunWith(AndroidJUnit4::class)
class BatonRecoveryIntegrationTest {

    private val reducer = BatonSyncReducer()
    private val me = "user-a"
    private val other = "user-b"
    private val third = "user-c"

    private fun request(identity: String, ts: Long, batonId: String = "b-$identity-$ts") =
        BatonRequest(batonId = batonId, identity = identity, ts = ts)

    @Test
    fun holderDisconnect_passesToNextInQueue() {
        // other holds, me and third are queued
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me, ts = 2_000L), request(third, ts = 4_000L)),
        )

        // Simulate recovery: holder (other) disconnected → pass to next (me)
        val next = state.pending.firstOrNull()
        val now = 10_000L
        val nextExpires = next?.let { now + BatonSyncReducer.TURN_MS }
        val result = reducer.onPass(state, next?.identity, nextExpires)

        assertEquals(me, result.state.holder)
        assertEquals(nextExpires, result.state.expiresAtEpochMs)
        // me removed from pending
        assertEquals(listOf(third), result.state.pending.map { it.identity })
    }

    @Test
    fun holderDisconnect_withEmptyQueue_dropsToIdle() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = emptyList(),
        )

        val result = reducer.onPass(state, null, null)

        assertNull(result.state.holder)
        assertNull(result.state.expiresAtEpochMs)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun holderExpiry_withQueue_passesToNext() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 10_000L,
            pending = listOf(request(me, ts = 2_000L)),
        )

        // other's turn expired → onExpiry is called by other (holder)
        // Simulate: holder expired, pass to me
        val result = reducer.onExpiry(state, other, 11_000L, BatonSyncReducer.TURN_MS)

        assertEquals(me, result.state.holder)
        assertEquals(1, result.commands.size)
    }

    @Test
    fun grantAfterRecovery_removesWinnerFromPending() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me, ts = 2_000L), request(third, ts = 4_000L)),
        )

        // Recovery pass to me
        val next = state.pending.firstOrNull()
        val now = 10_000L
        val result1 = reducer.onPass(state, next?.identity, now + BatonSyncReducer.TURN_MS)
        assertEquals(me, result1.state.holder)

        // Now me (new holder) passes to third
        val result2 = reducer.onPass(result1.state, third, now + BatonSyncReducer.TURN_MS * 2)
        assertEquals(third, result2.state.holder)
        assertTrue(result2.state.pending.isEmpty())
    }
}
