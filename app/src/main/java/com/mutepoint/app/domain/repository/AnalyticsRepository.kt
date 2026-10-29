package com.mutepoint.app.domain.repository

import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.model.SessionAnalytics
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {

    fun observeSession(sessionId: String): Flow<SessionAnalytics?>

    fun observeRecentSessions(limit: Int): Flow<List<SessionAnalytics>>

    suspend fun startSession(sessionId: String, startedAtMs: Long)

    suspend fun endSession(sessionId: String, endedAtMs: Long)

    suspend fun recordSpeakingTime(sessionId: String, speakingMillis: Long)

    suspend fun recordInterruption(sessionId: String, event: InterruptionEvent)

    suspend fun recentSessions(limit: Int): List<SessionAnalytics>
}
