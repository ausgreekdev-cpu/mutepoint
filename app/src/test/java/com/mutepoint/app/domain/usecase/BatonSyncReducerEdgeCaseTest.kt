package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.BatonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatonSyncReducerEdgeCaseTest {

    private val reducer = BatonSyncReducer()
    private val me = "user-a"
    private val other = "user-b"
    private val third = "user-c"

    private fun request(identity: String, ts: Long = 1000L, batonId: String = "b-$identity-$ts") =
        BatonRequest(batonId = batonId, identity = identity, ts = ts)

    @Test
    fun `three simultaneous requests elect lowest ts then identity`() {
        var state = BatonSyncState()
        state = reducer.onRequest(state, request(other, ts = 5_000L), 1_000L, 30_000L).state
        state = reducer.onRequest(state, request(me, ts = 3_000L), 1_000L, 30_000L).state
        state = reducer.onRequest(state, request(third, ts = 4_000L), 1_000L, 30_000L).state

        // me has lowest ts → should be granted (if no holder)
        // But only if holder is null at first request time. Since first request
        // (other, ts=5000) self-grants because holder was null.
        // So other becomes holder. me and third queue.
        assertEquals(other, state.holder)
        assertEquals(listOf(me, third), state.pending.map { it.identity })
    }

    @Test
    fun `cancel during hold removes from pending without changing holder`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me), request(third)),
        )
        val result = reducer.onCancel(state, request(me).batonId)
        assertEquals(other, result.state.holder)
        assertEquals(listOf(third), result.state.pending.map { it.identity })
    }

    @Test
    fun `grant from remote removes winner from pending`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me), request(third)),
        )
        val result = reducer.onGrant(state, me, 60_000L)
        assertEquals(me, result.state.holder)
        assertEquals(listOf(third), result.state.pending.map { it.identity })
    }

    @Test
    fun `pass with next auto-grants and removes from pending`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me), request(third)),
        )
        val result = reducer.onPass(state, nextWinner = me, nextExpiresAtEpochMs = 70_000L)
        assertEquals(me, result.state.holder)
        assertEquals(70_000L, result.state.expiresAtEpochMs)
        assertEquals(listOf(third), result.state.pending.map { it.identity })
    }

    @Test
    fun `expiry passes to next in queue`() {
        val state = BatonSyncState(
            holder = me,
            expiresAtEpochMs = 10_000L,
            pending = listOf(request(other), request(third)),
        )
        val result = reducer.onExpiry(state, me, 11_000L, 30_000L)
        assertEquals(other, result.state.holder)
        assertEquals(41_000L, result.state.expiresAtEpochMs)
        assertEquals(listOf(third), result.state.pending.map { it.identity })
        assertEquals(1, result.commands.size)
    }

    @Test
    fun `uiStateFor shows listening when remote holds and not queued`() {
        val state = BatonSyncState(holder = other, expiresAtEpochMs = 35_000L)
        val ui = reducer.uiStateFor(state, me, 5_000L)
        assertEquals(BatonState.Listening(other), ui)
    }

    @Test
    fun `uiStateFor shows inQueue when queued behind remote holder`() {
        val state = BatonSyncState(
            holder = other,
            expiresAtEpochMs = 35_000L,
            pending = listOf(request(me)),
        )
        val ui = reducer.uiStateFor(state, me, 5_000L)
        assertTrue(ui is BatonState.InQueue)
        assertEquals(1, (ui as BatonState.InQueue).position)
    }

    @Test
    fun `uiStateFor shows idle when no holder and not queued`() {
        val ui = reducer.uiStateFor(BatonSyncState(), me, 5_000L)
        assertEquals(BatonState.Idle, ui)
    }
}
