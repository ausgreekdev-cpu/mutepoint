package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.BatonState

data class BatonRequest(
    val batonId: String,
    val identity: String,
    val ts: Long,
)

data class BatonSyncState(
    val holder: String? = null,
    val expiresAtEpochMs: Long? = null,
    val pending: List<BatonRequest> = emptyList(),
)

sealed interface BatonSyncCommand {
    data class BroadcastGrant(
        val winner: String,
        val expiresAtEpochMs: Long,
    ) : BatonSyncCommand

    data class BroadcastPass(
        val nextWinner: String?,
        val nextExpiresAtEpochMs: Long?,
    ) : BatonSyncCommand
}

data class BatonSyncResult(
    val state: BatonSyncState,
    val commands: List<BatonSyncCommand> = emptyList(),
)

/**
 * Pure, deterministic baton state machine shared by every room participant.
 *
 * All decisions are computed from the same reliable, ordered data-channel
 * history, so every device reaches the same winner without a central server:
 * - the requester grants themselves the floor when nobody holds it
 * - the current holder is the only authority for Pass (including turn
 *   expiry) and embeds the next queued winner in the Pass message
 */
class BatonSyncReducer {

    fun onRequest(
        state: BatonSyncState,
        request: BatonRequest,
        nowMs: Long,
        turnMs: Long,
    ): BatonSyncResult {
        if (state.holder == request.identity) {
            return BatonSyncResult(state)
        }
        if (state.pending.any { it.batonId == request.batonId }) {
            return BatonSyncResult(state)
        }
        val pending = (state.pending + request).sortedWith(comparator)
        if (state.holder == null && pending.first() == request) {
            val expiresAt = nowMs + turnMs
            return BatonSyncResult(
                state = state.copy(
                    holder = request.identity,
                    expiresAtEpochMs = expiresAt,
                    pending = pending.filterNot { it.batonId == request.batonId },
                ),
                commands = listOf(BatonSyncCommand.BroadcastGrant(request.identity, expiresAt)),
            )
        }
        return BatonSyncResult(state.copy(pending = pending))
    }

    fun onCancel(state: BatonSyncState, batonId: String): BatonSyncResult =
        BatonSyncResult(state.copy(pending = state.pending.filterNot { it.batonId == batonId }))

    fun onGrant(state: BatonSyncState, winner: String, expiresAtEpochMs: Long): BatonSyncResult =
        BatonSyncResult(
            state = state.copy(
                holder = winner,
                expiresAtEpochMs = expiresAtEpochMs,
                pending = state.pending.filterNot { it.identity == winner },
            ),
        )

    fun onPass(
        state: BatonSyncState,
        nextWinner: String?,
        nextExpiresAtEpochMs: Long?,
    ): BatonSyncResult = BatonSyncResult(
        state = state.copy(
            holder = nextWinner,
            expiresAtEpochMs = if (nextWinner != null) nextExpiresAtEpochMs else null,
            pending = if (nextWinner != null) {
                state.pending.filterNot { it.identity == nextWinner }
            } else {
                state.pending
            },
        ),
    )

    /** Holder-authoritative turn expiry: acts as a Pass to the next in queue. */
    fun onExpiry(
        state: BatonSyncState,
        myIdentity: String,
        nowMs: Long,
        turnMs: Long,
    ): BatonSyncResult {
        if (state.holder != myIdentity) return BatonSyncResult(state)
        val expiresAt = state.expiresAtEpochMs ?: return BatonSyncResult(state)
        if (expiresAt > nowMs) return BatonSyncResult(state)
        val next = state.pending.firstOrNull()
        return BatonSyncResult(
            state = state.copy(
                holder = next?.identity,
                expiresAtEpochMs = next?.let { nowMs + turnMs },
                pending = state.pending.drop(1),
            ),
            commands = listOf(
                BatonSyncCommand.BroadcastPass(
                    nextWinner = next?.identity,
                    nextExpiresAtEpochMs = next?.let { nowMs + turnMs },
                ),
            ),
        )
    }

    fun rankOf(state: BatonSyncState, identity: String): Int? =
        state.pending.indexOfFirst { it.identity == identity }.takeIf { it >= 0 }?.plus(1)

    fun uiStateFor(state: BatonSyncState, myIdentity: String, nowMs: Long): BatonState {
        val holder = state.holder
        if (holder == myIdentity) {
            val remainingMs = (state.expiresAtEpochMs ?: nowMs) - nowMs
            return BatonState.HoldingBaton(
                timeRemainingSeconds = (remainingMs / 1000).toInt().coerceAtLeast(0),
            )
        }
        val rank = rankOf(state, myIdentity)
        if (rank != null) {
            return BatonState.InQueue(position = rank, estimatedWaitSeconds = rank * TURN_SECONDS)
        }
        return if (holder != null) {
            BatonState.Listening(activeSpeakerName = holder)
        } else {
            BatonState.Idle
        }
    }

    companion object {
        const val TURN_SECONDS = 30
        val TURN_MS: Long = TURN_SECONDS * 1000L
        private val comparator = compareBy<BatonRequest>({ it.ts }, { it.identity })
    }
}
