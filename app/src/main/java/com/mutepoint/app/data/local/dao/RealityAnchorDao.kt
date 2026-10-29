package com.mutepoint.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.mutepoint.app.data.local.entities.RealityAnchorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RealityAnchorDao {

    @Query("SELECT * FROM reality_anchor_entries ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<RealityAnchorEntity>>

    @Insert
    suspend fun insert(entity: RealityAnchorEntity): Long

    @Query("DELETE FROM reality_anchor_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
