package com.mutepoint.app.data.local.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "interruption_events",
    foreignKeys = [
        ForeignKey(
            entity = MeetingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionRowId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionRowId"])],
)
data class InterruptionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionRowId: Long,
    val timestampMs: Long,
    val durationMillis: Long,
    val speakerVolumeDb: Float,
    val interruptionVolumeDb: Float,
)
