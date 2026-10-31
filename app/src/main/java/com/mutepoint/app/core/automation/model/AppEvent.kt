package com.mutepoint.app.core.automation.model

/**
 * App-wide events that can trigger workflows.
 */
sealed class AppEvent {
    data class RoomJoined(val roomName: String) : AppEvent()
    data class RoomLeft(val roomName: String) : AppEvent()
    data class InterruptionDetected(val durationMs: Long, val db: Float) : AppEvent()
    data class SessionEnded(val sessionId: String, val speakingMs: Long, val interruptionCount: Int) : AppEvent()
    data class JournalEntryCreated(val entryId: String) : AppEvent()
    data object AppOpened : AppEvent()
}
