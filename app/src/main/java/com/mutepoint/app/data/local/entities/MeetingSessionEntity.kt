package com.mutepoint.app.data.local.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "meeting_sessions",
    indices = [Index(value = ["sessionId"], unique = true)],
)
data class MeetingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val speakingTimeMs: Long = 0L,
    val listeningTimeMs: Long = 0L,
    val timeCutOffMs: Long = 0L,
)
