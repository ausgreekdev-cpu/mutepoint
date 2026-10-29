package com.mutepoint.app.domain.repository

import com.mutepoint.app.domain.model.JournalEntry
import com.mutepoint.app.domain.model.PracticeCard
import kotlinx.coroutines.flow.Flow

interface PracticeRepository {
    fun observeCards(): Flow<List<PracticeCard>>

    fun observeJournal(): Flow<List<JournalEntry>>

    suspend fun insertJournalEntry(entry: JournalEntry): Long

    suspend fun deleteJournalEntry(id: Long)
}
