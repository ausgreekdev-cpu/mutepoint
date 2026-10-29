package com.mutepoint.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.mutepoint.app.data.local.entities.InterruptionEventEntity
import com.mutepoint.app.data.local.entities.MeetingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    @Query("SELECT * FROM meeting_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): MeetingSessionEntity?

    @Query("SELECT * FROM meeting_sessions WHERE sessionId = :sessionId LIMIT 1")
    fun observeSession(sessionId: String): Flow<MeetingSessionEntity?>

    @Query("SELECT * FROM meeting_sessions ORDER BY startedAtMs DESC LIMIT :limit")
    suspend fun recentSessions(limit: Int): List<MeetingSessionEntity>

    @Query("SELECT * FROM meeting_sessions ORDER BY startedAtMs DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<MeetingSessionEntity>>

    @Query("SELECT * FROM interruption_events WHERE sessionRowId = :sessionRowId ORDER BY timestampMs")
    suspend fun getEvents(sessionRowId: Long): List<InterruptionEventEntity>

    @Query("SELECT * FROM interruption_events WHERE sessionRowId = :sessionRowId ORDER BY timestampMs")
    fun observeEvents(sessionRowId: Long): Flow<List<InterruptionEventEntity>>

    @Query("SELECT COUNT(*) FROM interruption_events WHERE sessionRowId = :sessionRowId")
    suspend fun countEvents(sessionRowId: Long): Int

    @Insert
    suspend fun insertSession(entity: MeetingSessionEntity): Long

    @Update
    suspend fun updateSession(entity: MeetingSessionEntity)

    @Insert
    suspend fun insertEvent(entity: InterruptionEventEntity)
}
