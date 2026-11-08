package com.mutepoint.app.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.service.CheckInManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val checkInManager: CheckInManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(1000)
                refresh()
            }
        }
    }

    fun refresh() {
        val active = checkInManager.isActive()
        val dueTime = checkInManager.getDueTime()
        val remainingMs = if (active && dueTime > 0) (dueTime - System.currentTimeMillis()).coerceAtLeast(0) else 0L
        _uiState.value = _uiState.value.copy(
            isActive = active && remainingMs > 0,
            dueTime = dueTime,
            remainingMs = remainingMs,
        )
    }

    fun startCheckIn(minutes: Int) {
        checkInManager.scheduleCheckIn(minutes)
        refresh()
    }

    fun cancelCheckIn() {
        checkInManager.cancelCheckIn()
        refresh()
    }

    fun checkInNow() {
        checkInManager.cancelCheckIn()
        refresh()
    }
}

data class CheckInUiState(
    val isActive: Boolean = false,
    val dueTime: Long = 0L,
    val remainingMs: Long = 0L,
)
