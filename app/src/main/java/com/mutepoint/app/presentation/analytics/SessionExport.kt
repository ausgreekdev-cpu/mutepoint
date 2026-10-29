package com.mutepoint.app.presentation.analytics

import com.mutepoint.app.domain.model.SessionAnalytics
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatSessionExport(session: SessionAnalytics): String = buildString {
    appendLine("Mute Point session export")
    appendLine("Started: ${formatDate(session.startedAtMs)}")
    val totalSec = session.totalSpeakingDurationSec + session.totalListeningDurationSec
    appendLine("Length: ${formatDuration(totalSec * 1000L)}")
    appendLine(
        "You spoke: ${formatDuration(session.totalSpeakingDurationSec * 1000L)} " +
            "(%.0f%%)".format(session.speakingPercent),
    )
    appendLine("Cut off: ${formatDuration(session.timeCutOffMillis)}")
    appendLine("Interruptions: ${session.interruptionCount}")
    appendLine()
    if (session.interruptions.isEmpty()) {
        appendLine("None recorded.")
    } else {
        session.interruptions.forEachIndexed { index, event ->
            val offsetMs = (event.timestamp.toEpochMilli() - session.startedAtMs).coerceAtLeast(0L)
            appendLine(
                "#${index + 1} at ${formatOffset(offsetMs)} — " +
                    "${formatDuration(event.durationMillis)} · " +
                    "you ${event.speakerVolumeDb.toInt()} dB / " +
                    "them ${event.interruptionVolumeDb.toInt()} dB",
            )
        }
    }
}

internal fun formatOffset(millis: Long): String {
    val duration = Duration.ofMillis(millis)
    val minutes = duration.toMinutes()
    val seconds = duration.seconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatExportTimestamp(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
