package com.mutepoint.app.domain.model

sealed interface BatonState {
    data object Idle : BatonState

    data class InQueue(
        val position: Int,
        val estimatedWaitSeconds: Int,
    ) : BatonState

    data class HoldingBaton(
        val timeRemainingSeconds: Int,
    ) : BatonState

    data class Listening(
        val activeSpeakerName: String,
    ) : BatonState
}
