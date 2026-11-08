package com.mutepoint.app.data.local.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val priority: Int = 0,
    val isPrimary: Boolean = false,
    val createdAt: Long,
)