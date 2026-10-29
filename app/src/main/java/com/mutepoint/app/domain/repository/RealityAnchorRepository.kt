package com.mutepoint.app.domain.repository

import com.mutepoint.app.domain.model.RealityAnchorEntry
import kotlinx.coroutines.flow.Flow

interface RealityAnchorRepository {
    fun observeEntries(): Flow<List<RealityAnchorEntry>>

    suspend fun insertEntry(entry: RealityAnchorEntry): Long

    suspend fun deleteEntry(id: Long)
}
