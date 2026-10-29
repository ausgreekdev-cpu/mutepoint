package com.mutepoint.app.presentation.analytics

import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.model.SessionAnalytics
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExportTest {

    private fun session(
        interruptions: List<InterruptionEvent> = emptyList(),
        speakingSec: Long = 100,
        listeningSec: Long = 50,
    ) = SessionAnalytics(
        sessionId = "s1",
        startedAtMs = 1_700_000_000_000L,
        totalSpeakingDurationSec = speakingSec,
        totalListeningDurationSec = listeningSec,
        timeCutOffMillis = 12_000L,
        interruptionCount = interruptions.size,
        interruptions = interruptions,
    )

    @Test
    fun `empty session exports header and none recorded`() {
        val text = formatSessionExport(session())
        assertTrue(text.startsWith("Mute Point session export"))
        assertTrue(text.contains("Interruptions: 0"))
        assertTrue(text.contains("None recorded."))
        assertTrue(text.contains("Cut off: 0m 12s"))
    }

    @Test
    fun `session with interruptions lists each event`() {
        val events = listOf(
            InterruptionEvent(
                timestamp = Instant.ofEpochMilli(1_700_000_003_000L),
                durationMillis = 900L,
                speakerVolumeDb = -21.4f,
                interruptionVolumeDb = -7.2f,
            ),
            InterruptionEvent(
                timestamp = Instant.ofEpochMilli(1_700_000_015_000L),
                durationMillis = 2_000L,
                speakerVolumeDb = -18f,
                interruptionVolumeDb = -9f,
            ),
        )
        val text = formatSessionExport(session(interruptions = events))
        assertTrue(text.contains("Interruptions: 2"))
        assertTrue(text.contains("#1 at 0:03 — 0m 0s · you -21 dB / them -7 dB"))
        assertTrue(text.contains("#2 at 0:15 — 0m 2s · you -18 dB / them -9 dB"))
    }

    @Test
    fun `speaking percent is computed from durations`() {
        val text = formatSessionExport(session(speakingSec = 75, listeningSec = 25))
        assertTrue(text.contains("You spoke: 1m 15s (75%)"))
    }

    @Test
    fun `formatOffset pads seconds`() {
        assertEquals("0:00", formatOffset(0L))
        assertEquals("1:05", formatOffset(65_000L))
        assertEquals("12:04", formatOffset(724_000L))
    }
}
