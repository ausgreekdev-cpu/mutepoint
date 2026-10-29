package com.mutepoint.app.domain.model

data class SessionAnalytics(
    val sessionId: String,
    val startedAtMs: Long,
    val totalSpeakingDurationSec: Long,
    val totalListeningDurationSec: Long,
    val timeCutOffMillis: Long,
    val interruptionCount: Int,
    val interruptions: List<InterruptionEvent>,
) {
    val speakingPercent: Float
        get() {
            val total = totalSpeakingDurationSec + totalListeningDurationSec
            return if (total <= 0L) 0f else (totalSpeakingDurationSec.toFloat() / total) * 100f
        }
}
