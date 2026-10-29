package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.BatonState

class BatonManager(
    private val defaultTurnSeconds: Int = 30,
    private val queuePosition: Int = 1,
) {
    var state: BatonState = BatonState.Idle
        private set

    fun requestBaton(): BatonState {
        state = when (state) {
            is BatonState.Idle, is BatonState.Listening ->
                BatonState.InQueue(
                    position = queuePosition,
                    estimatedWaitSeconds = defaultTurnSeconds,
                )
            else -> state
        }
        return state
    }

    fun grantBaton(timeSeconds: Int = defaultTurnSeconds): BatonState {
        if (state is BatonState.InQueue) {
            state = BatonState.HoldingBaton(timeRemainingSeconds = timeSeconds)
        }
        return state
    }

    fun tick(): BatonState {
        val current = state
        state = if (current is BatonState.HoldingBaton) {
            val remaining = current.timeRemainingSeconds - 1
            if (remaining > 0) {
                BatonState.HoldingBaton(timeRemainingSeconds = remaining)
            } else {
                BatonState.Idle
            }
        } else {
            current
        }
        return state
    }

    fun passBaton(): BatonState {
        state = when (state) {
            is BatonState.HoldingBaton, is BatonState.InQueue -> BatonState.Idle
            else -> state
        }
        return state
    }

    fun leaveQueue(): BatonState {
        if (state is BatonState.InQueue) {
            state = BatonState.Idle
        }
        return state
    }
}
