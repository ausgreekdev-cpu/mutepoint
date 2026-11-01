package com.mutepoint.app.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.EmergencyContact
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    val contacts = contactsRepository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            val contact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                relationship = relationship.trim(),
                priority = contacts.value.size,
                isPrimary = contacts.value.isEmpty(),
                createdAt = System.currentTimeMillis(),
            )
            contactsRepository.addContact(contact)
        }
    }

    fun removeContact(contact: EmergencyContact) {
        viewModelScope.launch {
            contactsRepository.removeContact(contact)
        }
    }
}