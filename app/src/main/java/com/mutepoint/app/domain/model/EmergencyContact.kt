package com.mutepoint.app.domain.model

data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val priority: Int = 0,
    val isPrimary: Boolean = false,
    val createdAt: Long,
)