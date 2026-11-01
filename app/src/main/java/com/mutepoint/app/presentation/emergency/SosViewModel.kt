package com.mutepoint.app.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.service.SosManager
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosManager: SosManager,
    contactsRepository: ContactsRepository,
) : ViewModel() {

    val contactsCount = contactsRepository.observeContacts()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sendSos() {
        viewModelScope.launch {
            sosManager.sendSos()
        }
    }
}