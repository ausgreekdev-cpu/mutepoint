package com.mutepoint.app.domain.repository

import com.mutepoint.app.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun observeContacts(): Flow<List<EmergencyContact>>
    suspend fun getContacts(): List<EmergencyContact>
    suspend fun getPrimary(): EmergencyContact?
    suspend fun addContact(contact: EmergencyContact)
    suspend fun updateContact(contact: EmergencyContact)
    suspend fun removeContact(contact: EmergencyContact)
}