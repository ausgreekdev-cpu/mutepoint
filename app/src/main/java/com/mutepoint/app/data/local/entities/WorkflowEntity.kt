package com.mutepoint.app.data.local.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val triggerType: String,
    val triggerConfig: String,   // JSON
    val conditions: String,      // JSON array
    val actions: String,         // JSON array
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "workflow_executions")
data class WorkflowExecutionEntity(
    @PrimaryKey val id: String,
    val workflowId: String,
    val triggeredAt: Long,
    val result: String,          // "success", "failed", "skipped"
    val durationMs: Long,
    val error: String? = null,
)
