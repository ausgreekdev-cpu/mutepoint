package com.mutepoint.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Delete
import com.mutepoint.app.data.local.entities.WorkflowEntity
import com.mutepoint.app.data.local.entities.WorkflowExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    @Query("SELECT * FROM workflows ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE enabled = 1")
    suspend fun getEnabled(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkflowEntity)

    @Delete
    suspend fun delete(entity: WorkflowEntity)

    @Query("SELECT * FROM workflow_executions WHERE workflowId = :workflowId ORDER BY triggeredAt DESC LIMIT :limit")
    suspend fun getExecutions(workflowId: String, limit: Int): List<WorkflowExecutionEntity>

    @Insert
    suspend fun insertExecution(entity: WorkflowExecutionEntity)
}
