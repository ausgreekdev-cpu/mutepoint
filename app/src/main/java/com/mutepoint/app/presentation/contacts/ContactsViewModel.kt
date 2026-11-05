package com.mutepoint.app.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.EmergencyContact
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    val contacts: StateFlow<List<EmergencyContact>> = contactsRepository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            val contact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                relationship = relationship.trim(),
                priority = if (isPrimary) 1 else 0,
                isPrimary = isPrimary,
                createdAt = System.currentTimeMillis(),
            )
            contactsRepository.addContact(contact)
            hideAddDialog()
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch {
            val contact = contacts.value.firstOrNull { it.id == id } ?: return@launch
            contactsRepository.removeContact(contact)
        }
    }

    fun setPrimary(id: String) {
        viewModelScope.launch {
            val contact = contacts.value.firstOrNull { it.id == id } ?: return@launch
            contactsRepository.updateContact(contact.copy(isPrimary = true, priority = 1))
        }
    }
}

data class ContactsUiState(
    val showAddDialog: Boolean = false,
)
