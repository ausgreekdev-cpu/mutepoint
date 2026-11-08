package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.SessionAnalytics

data class PatternInsights(
    val totalSessions: Int,
    val totalInterruptions: Int,
    val averageSpeakingPercent: Float,
    val averageInterruptionsPerSession: Float,
    val totalTimeCutOffMs: Long,
    val trend: InterruptionTrend,
    val summary: String,
)

enum class InterruptionTrend { IMPROVING, STABLE, WORSENING, INSUFFICIENT_DATA }

class PatternAnalyzer {
    fun analyze(sessions: List<SessionAnalytics>): PatternInsights {
        if (sessions.isEmpty()) {
            return PatternInsights(
                totalSessions = 0,
                totalInterruptions = 0,
                averageSpeakingPercent = 0f,
                averageInterruptionsPerSession = 0f,
                totalTimeCutOffMs = 0L,
                trend = InterruptionTrend.INSUFFICIENT_DATA,
                summary = "No session data yet. Start a session to see insights.",
            )
        }

        val totalSessions = sessions.size
        val totalInterruptions = sessions.sumOf { it.interruptionCount }
        val avgSpeaking = sessions.map { it.speakingPercent }.average().toFloat()
        val avgInterruptions = if (totalSessions > 0) totalInterruptions.toFloat() / totalSessions else 0f
        val totalCutOff = sessions.sumOf { it.timeCutOffMillis }

        val trend = calculateTrend(sessions)
        val summary = buildSummary(totalSessions, totalInterruptions, avgSpeaking, avgInterruptions, trend)

        return PatternInsights(
            totalSessions = totalSessions,
            totalInterruptions = totalInterruptions,
            averageSpeakingPercent = avgSpeaking,
            averageInterruptionsPerSession = avgInterruptions,
            totalTimeCutOffMs = totalCutOff,
            trend = trend,
            summary = summary,
        )
    }

    private fun calculateTrend(sessions: List<SessionAnalytics>): InterruptionTrend {
        if (sessions.size < 4) return InterruptionTrend.INSUFFICIENT_DATA
        val sorted = sessions.sortedBy { it.startedAtMs }
        val mid = sorted.size / 2
        val firstHalf = sorted.take(mid)
        val secondHalf = sorted.drop(mid)
        val firstAvg = if (firstHalf.isNotEmpty()) {
            firstHalf.sumOf { it.interruptionCount }.toFloat() / firstHalf.size
        } else 0f
        val secondAvg = if (secondHalf.isNotEmpty()) {
            secondHalf.sumOf { it.interruptionCount }.toFloat() / secondHalf.size
        } else 0f
        val diff = secondAvg - firstAvg
        return when {
            diff < -0.5f -> InterruptionTrend.IMPROVING
            diff > 0.5f -> InterruptionTrend.WORSENING
            else -> InterruptionTrend.STABLE
        }
    }

    private fun buildSummary(
        totalSessions: Int,
        totalInterruptions: Int,
        avgSpeaking: Float,
        avgInterruptions: Float,
        trend: InterruptionTrend,
    ): String {
        val trendText = when (trend) {
            InterruptionTrend.IMPROVING -> "Interruptions are trending down."
            InterruptionTrend.WORSENING -> "Interruptions are trending up — consider reviewing patterns."
            InterruptionTrend.STABLE -> "Interruption levels are stable."
            InterruptionTrend.INSUFFICIENT_DATA -> "More sessions needed to detect trends."
        }
        return "Across $totalSessions session${if (totalSessions == 1) "" else "s"}: " +
            "$totalInterruptions total interruptions, ${"%.1f".format(avgInterruptions)} avg per session, " +
            "${avgSpeaking.toInt()}% average speaking time. $trendText"
    }
}
