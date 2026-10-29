package com.mutepoint.app.data.livekit

/** Connection + publish state surfaced to the Safe Room UI. */
sealed interface LiveKitStatus {
    data object Disconnected : LiveKitStatus
    data object Connecting : LiveKitStatus
    data class Connected(
        val participantCount: Int,
        val remoteSpeakers: List<String>,
    ) : LiveKitStatus

    data class Error(val message: String) : LiveKitStatus
}
