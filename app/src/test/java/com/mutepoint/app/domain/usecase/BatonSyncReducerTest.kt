package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.BatonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatonSyncReducerTest {

    private val reducer = BatonSyncReducer()
    private val me = "user-a"
    private val other = "user-b"

    private fun request(
        identity: String,
        ts: Long = 1000L,
        batonId: String = "baton-$identity-$ts",
    ) = BatonRequest(batonId = batonId, identity = identity, ts = ts)

    @Test
    fun `request with idle room grants the requester and broadcasts grant`() {
        val result = reducer.onRequest(BatonSyncState(), request(me), nowMs = 5_000L, turnMs = 30_000L)
        assertEquals(me, result.state.holder)
        assertEquals(35_000L, result.state.expiresAtEpochMs)
        assertEquals(
            listOf(BatonSyncCommand.BroadcastGrant(me, 35_000L)),
            result.commands,
        )
    }

    @Test
    fun `request while another holds queues the requester`() {
        val held = BatonSyncState(holder = other, expiresAtEpochMs = 35_000L)
        val result = reducer.onRequest(held, request(me), nowMs = 5_000L, turnMs = 30_000L)
        assertEquals(other, result.state.holder)
        assertEquals(listOf(request(me)), result.state.pending)
        assertTrue(result.commands.isEmpty())
    }

    @Test
    fun `pending orders by ts then identity regardless of arrival order`() {
        val holder = "user-x"
        var state = BatonSyncState(holder = holder, expiresAtEpochMs = 35_000L)
        state = reducer.onRequest(state, request(other, ts = 3_000L), 5_000L, 30_000L).state
        state = reducer.onRequest(state, request(me, ts = 2_000L), 5_000L, 30_000L).state
        assertEquals(listOf(me, other), state.pending.map { it.identity })
        assertEquals(1, reducer.rankOf(state, me))
        assertEquals(2, reducer.rankOf(state, other))
    }

    @Test
    fun `grant removes winner from pending and sets holder`() {
        val queued = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me)),
        )
        val result = reducer.onGrant(queued, me, 60_000L)
        assertEquals(me, result.state.holder)
        assertEquals(60_000L, result.state.expiresAtEpochMs)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun `pass with queued requester auto-grants the next winner`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me, ts = 2_000L)),
        )
        val result = reducer.onPass(state, nextWinner = me, nextExpiresAtEpochMs = 70_000L)
        assertEquals(me, result.state.holder)
        assertEquals(70_000L, result.state.expiresAtEpochMs)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun `pass with empty queue drops everyone to idle`() {
        val state = BatonSyncState(holder = other, expiresAtEpochMs = 35_000L)
        val result = reducer.onPass(state, nextWinner = null, nextExpiresAtEpochMs = null)
        assertNull(result.state.holder)
        assertNull(result.state.expiresAtEpochMs)
    }

    @Test
    fun `holder expiry passes baton to next in queue`() {
        val state = BatonSyncState(
            holder = me,
            expiresAtEpochMs = 10_000L,
            pending = listOf(request(other, ts = 4_000L)),
        )
        val result = reducer.onExpiry(state, myIdentity = me, nowMs = 11_000L, turnMs = 30_000L)
        assertEquals(other, result.state.holder)
        assertEquals(41_000L, result.state.expiresAtEpochMs)
        assertEquals(
            listOf(BatonSyncCommand.BroadcastPass(other, 41_000L)),
            result.commands,
        )
    }

    @Test
    fun `expiry is ignored before the deadline and for non-holders`() {
        val state = BatonSyncState(holder = me, expiresAtEpochMs = 20_000L)
        assertEquals(state, reducer.onExpiry(state, me, nowMs = 19_000L, turnMs = 30_000L).state)
        assertEquals(state, reducer.onExpiry(state, other, nowMs = 21_000L, turnMs = 30_000L).state)
    }

    @Test
    fun `duplicate request by batonId is a no-op`() {
        val r = request(me)
        val state = reducer.onRequest(BatonSyncState(), r, 5_000L, 30_000L).state
        val again = reducer.onRequest(state, r, 6_000L, 30_000L)
        assertEquals(state, again.state)
        assertTrue(again.commands.isEmpty())
    }

    @Test
    fun `cancel removes the matching pending request`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me)),
        )
        val result = reducer.onCancel(state, request(me).batonId)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun `uiStateFor maps holder, listener, queue and idle`() {
        val held = BatonSyncState(holder = me, expiresAtEpochMs = 15_000L)
        val mine = reducer.uiStateFor(held, me, nowMs = 5_000L)
        assertTrue(mine is BatonState.HoldingBaton)
        assertEquals(10, (mine as BatonState.HoldingBaton).timeRemainingSeconds)

        val theirs = reducer.uiStateFor(held, other, nowMs = 5_000L)
        assertEquals(BatonState.Listening(me), theirs)

        val queued = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me)),
        )
        val queueUi = reducer.uiStateFor(queued, me, nowMs = 5_000L)
        assertTrue(queueUi is BatonState.InQueue)
        assertEquals(1, (queueUi as BatonState.InQueue).position)

        assertEquals(BatonState.Idle, reducer.uiStateFor(BatonSyncState(), me, nowMs = 5_000L))
    }
}
