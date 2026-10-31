package com.mutepoint.app.core.automation.repository

import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.model.WorkflowContext
import kotlinx.coroutines.flow.Flow

interface WorkflowRepository {

    fun observeWorkflows(): Flow<List<Workflow>>

    suspend fun getWorkflow(id: String): Workflow?

    suspend fun saveWorkflow(workflow: Workflow)

    suspend fun deleteWorkflow(id: String)

    suspend fun toggleEnabled(id: String, enabled: Boolean)

    suspend fun getExecutionHistory(workflowId: String, limit: Int = 20): List<WorkflowExecution>
}

data class WorkflowExecution(
    val id: String,
    val workflowId: String,
    val triggeredAt: Long,
    val result: String,
    val durationMs: Long,
    val error: String? = null,
)
