package com.mutepoint.app.data.local.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "reality_anchor_entries")
data class RealityAnchorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val triggerText: String,
    val realityText: String,
)
