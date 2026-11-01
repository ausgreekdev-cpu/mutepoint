package com.mutepoint.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.mutepoint.app.data.local.entities.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactsDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, createdAt ASC")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, createdAt ASC")
    suspend fun getAll(): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(): EmergencyContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmergencyContactEntity)

    @Delete
    suspend fun delete(entity: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAll()
}