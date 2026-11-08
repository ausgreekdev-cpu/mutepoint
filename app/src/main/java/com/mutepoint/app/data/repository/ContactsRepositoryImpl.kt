package com.mutepoint.app.data.repository

import com.mutepoint.app.data.local.dao.ContactsDao
import com.mutepoint.app.data.local.entities.EmergencyContactEntity
import com.mutepoint.app.domain.model.EmergencyContact
import com.mutepoint.app.domain.repository.ContactsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val contactsDao: ContactsDao,
) : ContactsRepository {

    override fun observeContacts(): Flow<List<EmergencyContact>> =
        contactsDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getContacts(): List<EmergencyContact> =
        contactsDao.getAll().map { it.toDomain() }

    override suspend fun getPrimary(): EmergencyContact? =
        contactsDao.getPrimary()?.toDomain()

    override suspend fun addContact(contact: EmergencyContact) {
        contactsDao.upsert(contact.toEntity())
    }

    override suspend fun updateContact(contact: EmergencyContact) {
        contactsDao.upsert(contact.toEntity())
    }

    override suspend fun removeContact(contact: EmergencyContact) {
        contactsDao.delete(contact.toEntity())
    }

    private fun EmergencyContactEntity.toDomain() = EmergencyContact(
        id = id,
        name = name,
        phone = phone,
        relationship = relationship,
        priority = priority,
        isPrimary = isPrimary,
        createdAt = createdAt,
    )

    private fun EmergencyContact.toEntity() = EmergencyContactEntity(
        id = id,
        name = name,
        phone = phone,
        relationship = relationship,
        priority = priority,
        isPrimary = isPrimary,
        createdAt = createdAt,
    )
}