package com.mutepoint.app.data.repository

import com.mutepoint.app.data.local.dao.AnalyticsDao
import com.mutepoint.app.data.local.entities.InterruptionEventEntity
import com.mutepoint.app.data.local.entities.MeetingSessionEntity
import com.mutepoint.app.data.mapper.toDomain
import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.repository.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
) : AnalyticsRepository {

    override fun observeSession(sessionId: String): Flow<SessionAnalytics?> =
        analyticsDao.observeSession(sessionId).mapLatest { session ->
            if (session == null) {
                null
            } else {
                session.toDomain(analyticsDao.getEvents(session.id).map { it.toDomain() })
            }
        }

    override fun observeRecentSessions(limit: Int): Flow<List<SessionAnalytics>> =
        analyticsDao.observeRecentSessions(limit).mapLatest { sessions ->
            sessions.map { session ->
                session.toDomain(analyticsDao.getEvents(session.id).map { it.toDomain() })
            }
        }

    override suspend fun startSession(sessionId: String, startedAtMs: Long) {
        if (analyticsDao.getSession(sessionId) != null) return
        analyticsDao.insertSession(
            MeetingSessionEntity(
                sessionId = sessionId,
                startedAtMs = startedAtMs,
            ),
        )
    }

    override suspend fun endSession(sessionId: String, endedAtMs: Long) {
        val session = analyticsDao.getSession(sessionId) ?: return
        analyticsDao.updateSession(session.copy(endedAtMs = endedAtMs))
    }

    override suspend fun recordSpeakingTime(sessionId: String, speakingMillis: Long) {
        val session = analyticsDao.getSession(sessionId) ?: return
        analyticsDao.updateSession(
            session.copy(speakingTimeMs = session.speakingTimeMs + speakingMillis),
        )
    }

    override suspend fun recordInterruption(sessionId: String, event: InterruptionEvent) {
        val session = analyticsDao.getSession(sessionId) ?: return
        analyticsDao.insertEvent(
            InterruptionEventEntity(
                sessionRowId = session.id,
                timestampMs = event.timestamp.toEpochMilli(),
                durationMillis = event.durationMillis,
                speakerVolumeDb = event.speakerVolumeDb,
                interruptionVolumeDb = event.interruptionVolumeDb,
            ),
        )
        analyticsDao.updateSession(
            session.copy(timeCutOffMs = session.timeCutOffMs + event.durationMillis),
        )
    }

    override suspend fun recentSessions(limit: Int): List<SessionAnalytics> =
        analyticsDao.recentSessions(limit).map { session ->
            val events = analyticsDao.getEvents(session.id).map { it.toDomain() }
            session.toDomain(events)
        }

    override suspend fun deleteOlderThan(cutoffMs: Long) {
        analyticsDao.deleteSessionsOlderThan(cutoffMs)
        analyticsDao.deleteOrphanedEvents()
    }
}
