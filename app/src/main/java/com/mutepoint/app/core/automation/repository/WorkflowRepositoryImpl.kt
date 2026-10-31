package com.mutepoint.app.core.automation.repository

import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.data.local.dao.WorkflowDao
import com.mutepoint.app.data.local.entities.WorkflowEntity
import com.mutepoint.app.data.local.entities.WorkflowExecutionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowRepositoryImpl @Inject constructor(
    private val workflowDao: WorkflowDao,
) : WorkflowRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeWorkflows(): Flow<List<Workflow>> =
        workflowDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getWorkflow(id: String): Workflow? =
        workflowDao.getById(id)?.toDomain()

    override suspend fun saveWorkflow(workflow: Workflow) {
        workflowDao.upsert(workflow.toEntity())
    }

    override suspend fun deleteWorkflow(id: String) {
        workflowDao.getById(id)?.let { workflowDao.delete(it) }
    }

    override suspend fun toggleEnabled(id: String, enabled: Boolean) {
        workflowDao.getById(id)?.let { entity ->
            workflowDao.upsert(entity.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun getExecutionHistory(workflowId: String, limit: Int): List<WorkflowExecution> =
        workflowDao.getExecutions(workflowId, limit).map { it.toDomain() }

    suspend fun recordExecution(execution: WorkflowExecution) {
        workflowDao.insertExecution(
            WorkflowExecutionEntity(
                id = execution.id,
                workflowId = execution.workflowId,
                triggeredAt = execution.triggeredAt,
                result = execution.result,
                durationMs = execution.durationMs,
                error = execution.error,
            )
        )
    }

    private fun WorkflowEntity.toDomain(): Workflow = Workflow(
        id = id,
        name = name,
        description = description,
        enabled = enabled,
        trigger = json.decodeFromString(triggerConfig),
        conditions = json.decodeFromString(conditions),
        actions = json.decodeFromString(actions),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Workflow.toEntity(): WorkflowEntity = WorkflowEntity(
        id = id,
        name = name,
        description = description,
        enabled = enabled,
        triggerType = trigger::class.simpleName ?: "Manual",
        triggerConfig = json.encodeToString(trigger),
        conditions = json.encodeToString(conditions),
        actions = json.encodeToString(actions),
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
    )

    private fun WorkflowExecutionEntity.toDomain(): WorkflowExecution = WorkflowExecution(
        id = id,
        workflowId = workflowId,
        triggeredAt = triggeredAt,
        result = result,
        durationMs = durationMs,
        error = error,
    )
}
