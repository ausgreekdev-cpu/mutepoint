package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.SessionAnalytics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnalyzerTest {

    private val analyzer = PatternAnalyzer()

    private fun session(id: String, startedAtMs: Long, interruptions: Int, speakingPercent: Float = 50f): SessionAnalytics {
        val total = 200L
        val speaking = (total * speakingPercent / 100).toLong()
        return SessionAnalytics(
            sessionId = id,
            startedAtMs = startedAtMs,
            totalSpeakingDurationSec = speaking,
            totalListeningDurationSec = total - speaking,
            timeCutOffMillis = 0,
            interruptionCount = interruptions,
            interruptions = emptyList(),
        )
    }

    @Test
    fun `empty sessions returns insufficient data`() {
        val result = analyzer.analyze(emptyList())
        assertEquals(0, result.totalSessions)
        assertEquals(InterruptionTrend.INSUFFICIENT_DATA, result.trend)
        assertEquals("No session data yet. Start a session to see insights.", result.summary)
    }

    @Test
    fun `single session computes basic metrics`() {
        val result = analyzer.analyze(listOf(session("s1", 1000L, 3, 75f)))
        assertEquals(1, result.totalSessions)
        assertEquals(3, result.totalInterruptions)
        assertEquals(75f, result.averageSpeakingPercent)
        assertEquals(3f, result.averageInterruptionsPerSession)
        assertEquals(InterruptionTrend.INSUFFICIENT_DATA, result.trend)
        assertTrue(result.summary.contains("1 session"))
    }

    @Test
    fun `multiple sessions compute averages and trend`() {
        val sessions = listOf(
            session("s1", 1000L, 5, 60f),
            session("s2", 2000L, 4, 65f),
            session("s3", 3000L, 2, 70f),
            session("s4", 4000L, 1, 75f),
        )
        val result = analyzer.analyze(sessions)
        assertEquals(4, result.totalSessions)
        assertEquals(12, result.totalInterruptions)
        assertEquals(3f, result.averageInterruptionsPerSession)
        assertEquals(InterruptionTrend.IMPROVING, result.trend)
    }

    @Test
    fun `worsening trend detected when interruptions increase`() {
        val sessions = listOf(
            session("s1", 1000L, 1, 50f),
            session("s2", 2000L, 2, 50f),
            session("s3", 3000L, 5, 50f),
            session("s4", 4000L, 6, 50f),
        )
        val result = analyzer.analyze(sessions)
        assertEquals(InterruptionTrend.WORSENING, result.trend)
    }

    @Test
    fun `stable trend when interruptions stay consistent`() {
        val sessions = listOf(
            session("s1", 1000L, 3, 50f),
            session("s2", 2000L, 3, 50f),
            session("s3", 3000L, 3, 50f),
            session("s4", 4000L, 3, 50f),
        )
        val result = analyzer.analyze(sessions)
        assertEquals(InterruptionTrend.STABLE, result.trend)
    }
}
