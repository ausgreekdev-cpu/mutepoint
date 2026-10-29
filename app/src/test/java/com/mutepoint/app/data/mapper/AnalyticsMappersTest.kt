package com.mutepoint.app.data.mapper

import com.mutepoint.app.data.local.entities.MeetingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsMappersTest {

    @Test
    fun `closed session listening is total minus speaking`() {
        val entity = MeetingSessionEntity(
            id = 1,
            sessionId = "s1",
            startedAtMs = 1_000_000L,
            endedAtMs = 1_000_000L + 120_000L,
            speakingTimeMs = 45_000L,
        )
        val domain = entity.toDomain(events = emptyList())
        assertEquals(45L, domain.totalSpeakingDurationSec)
        assertEquals(75L, domain.totalListeningDurationSec)
    }

    @Test
    fun `unclosed session does not accrue wall-clock listening time`() {
        val entity = MeetingSessionEntity(
            id = 2,
            sessionId = "s2",
            startedAtMs = 1_000_000L,
            endedAtMs = null,
            speakingTimeMs = 10_000L,
        )
        val domain = entity.toDomain(events = emptyList())
        assertEquals(0L, domain.totalListeningDurationSec)
        assertEquals(10L, domain.totalSpeakingDurationSec)
    }

    @Test
    fun `speaking longer than total clamps listening to zero`() {
        val entity = MeetingSessionEntity(
            id = 3,
            sessionId = "s3",
            startedAtMs = 1_000_000L,
            endedAtMs = 1_000_000L + 5_000L,
            speakingTimeMs = 9_000L,
        )
        val domain = entity.toDomain(events = emptyList())
        assertEquals(0L, domain.totalListeningDurationSec)
        assertEquals(9L, domain.totalSpeakingDurationSec)
    }
}
