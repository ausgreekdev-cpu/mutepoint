package com.mutepoint.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.mutepoint.app.data.local.dao.AnalyticsDao
import com.mutepoint.app.data.local.dao.CardsDao
import com.mutepoint.app.data.local.dao.JournalDao
import com.mutepoint.app.data.local.dao.RealityAnchorDao
import com.mutepoint.app.data.local.entities.AssertionCardEntity
import com.mutepoint.app.data.local.entities.InterruptionEventEntity
import com.mutepoint.app.data.local.entities.JournalEntryEntity
import com.mutepoint.app.data.local.entities.MeetingSessionEntity
import com.mutepoint.app.data.local.entities.RealityAnchorEntity
import com.mutepoint.app.data.local.entities.WorkflowEntity
import com.mutepoint.app.data.local.entities.WorkflowExecutionEntity

@Database(
    entities = [
        MeetingSessionEntity::class,
        InterruptionEventEntity::class,
        JournalEntryEntity::class,
        AssertionCardEntity::class,
        RealityAnchorEntity::class,
        WorkflowEntity::class,
        WorkflowExecutionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MutePointDatabase : RoomDatabase() {
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun journalDao(): JournalDao
    abstract fun cardsDao(): CardsDao
    abstract fun realityAnchorDao(): RealityAnchorDao
    abstract fun workflowDao(): WorkflowDao
}
