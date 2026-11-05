package com.mutepoint.app.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.service.SosManager
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosManager: SosManager,
    contactsRepository: ContactsRepository,
) : ViewModel() {

    val contactsCount = contactsRepository.observeContacts()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun sendSos(customMessage: String = "I need help. This is an emergency.") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            try {
                val result = sosManager.sendSos(customMessage)
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    lastResult = result,
                    showResult = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    lastResult = SosManager.SosResult(0, false, "Error: ${e.message}"),
                    showResult = true,
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(showResult = false)
    }

    fun getSosIntent() = sosManager.sendSosViaIntent()
}

data class SosUiState(
    val isSending: Boolean = false,
    val lastResult: SosManager.SosResult? = null,
    val showResult: Boolean = false,
)
