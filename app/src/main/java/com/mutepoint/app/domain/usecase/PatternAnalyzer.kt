package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.SessionAnalytics
import javax.inject.Inject

data class PatternInsight(
    val title: String,
    val description: String,
    val severity: String, // low, medium, high
)

class PatternAnalyzer @Inject constructor() {
    fun analyze(sessions: List<SessionAnalytics>): List<PatternInsight> {
        if (sessions.isEmpty()) return emptyList()
        val insights = mutableListOf<PatternInsight>()

        val totalInterruptions = sessions.sumOf { it.interruptionCount }
        val avgInterruptions = totalInterruptions.toFloat() / sessions.size
        if (avgInterruptions > 3f) {
            insights.add(
                PatternInsight(
                    title = "Frequent Interruptions",
                    description = "Average ${String.format("%.1f", avgInterruptions)} interruptions per session. Consider setting boundaries.",
                    severity = "high",
                ),
            )
        }

        val totalSpeaking = sessions.sumOf { it.speakingTimeMs }
        val totalDuration = sessions.sumOf { it.durationMs }
        if (totalDuration > 0) {
            val speakingRatio = totalSpeaking.toFloat() / totalDuration
            if (speakingRatio < 0.3f) {
                insights.add(
                    PatternInsight(
                        title = "Limited Speaking Time",
                        description = "You're speaking only ${String.format("%.0f", speakingRatio * 100)}% of session time.",
                        severity = "medium",
                    ),
                )
            }
        }

        val recentSessions = sessions.sortedByDescending { it.startedAtMs }.take(5)
        val recentAvg = recentSessions.sumOf { it.interruptionCount }.toFloat() / recentSessions.size
        if (recentAvg > avgInterruptions * 1.2f) {
            insights.add(
                PatternInsight(
                    title = "Increasing Trend",
                    description = "Interruptions are increasing in recent sessions compared to your average.",
                    severity = "high",
                ),
            )
        }

        if (insights.isEmpty()) {
            insights.add(
                PatternInsight(
                    title = "Good Progress",
                    description = "Your conversation patterns look balanced. Keep practicing assertive communication.",
                    severity = "low",
                ),
            )
        }

        return insights
    }
}