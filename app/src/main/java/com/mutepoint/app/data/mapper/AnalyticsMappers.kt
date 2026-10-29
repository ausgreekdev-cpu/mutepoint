package com.mutepoint.app.data.mapper

import com.mutepoint.app.data.local.entities.InterruptionEventEntity
import com.mutepoint.app.data.local.entities.JournalEntryEntity
import com.mutepoint.app.data.local.entities.MeetingSessionEntity
import com.mutepoint.app.data.local.entities.RealityAnchorEntity
import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.model.JournalEntry
import com.mutepoint.app.domain.model.RealityAnchorEntry
import com.mutepoint.app.domain.model.SessionAnalytics
import java.time.Instant

fun MeetingSessionEntity.toDomain(
    events: List<InterruptionEvent>,
): SessionAnalytics {
    // Unclosed sessions (crash before endSession) must not accrue listening
    // time against the wall clock — treat them as zero-length.
    val endedAt = endedAtMs ?: startedAtMs
    val totalMs = (endedAt - startedAtMs).coerceAtLeast(0L)
    val listeningMs = (totalMs - speakingTimeMs).coerceAtLeast(0L)
    return SessionAnalytics(
        sessionId = sessionId,
        startedAtMs = startedAtMs,
        totalSpeakingDurationSec = speakingTimeMs / 1000L,
        totalListeningDurationSec = listeningMs / 1000L,
        timeCutOffMillis = timeCutOffMs,
        interruptionCount = events.size,
        interruptions = events,
    )
}

fun InterruptionEventEntity.toDomain(): InterruptionEvent = InterruptionEvent(
    id = id,
    timestamp = Instant.ofEpochMilli(timestampMs),
    durationMillis = durationMillis,
    speakerVolumeDb = speakerVolumeDb,
    interruptionVolumeDb = interruptionVolumeDb,
)

fun JournalEntryEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    title = title,
    durationMillis = durationMillis,
    filePath = filePath,
    createdAt = Instant.ofEpochMilli(createdAtMs),
)

fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    title = title,
    durationMillis = durationMillis,
    filePath = filePath,
    createdAtMs = createdAt.toEpochMilli(),
)

fun RealityAnchorEntity.toDomain(): RealityAnchorEntry = RealityAnchorEntry(
    id = id,
    timestamp = Instant.ofEpochMilli(timestampMs),
    triggerText = triggerText,
    realityText = realityText,
)

fun RealityAnchorEntry.toEntity(): RealityAnchorEntity = RealityAnchorEntity(
    id = id,
    timestampMs = timestamp.toEpochMilli(),
    triggerText = triggerText,
    realityText = realityText,
)
