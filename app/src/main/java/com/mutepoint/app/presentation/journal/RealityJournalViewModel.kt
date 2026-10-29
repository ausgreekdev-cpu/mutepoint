package com.mutepoint.app.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.RealityAnchorEntry
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RealityJournalViewModel @Inject constructor(
    private val realityAnchorRepository: RealityAnchorRepository,
) : ViewModel() {

    val entries: StateFlow<List<RealityAnchorEntry>> = realityAnchorRepository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveEntry(trigger: String, reality: String) {
        val trimmedTrigger = trigger.trim()
        val trimmedReality = reality.trim()
        if (trimmedTrigger.isEmpty() || trimmedReality.isEmpty()) return
        viewModelScope.launch {
            realityAnchorRepository.insertEntry(
                RealityAnchorEntry(
                    triggerText = trimmedTrigger,
                    realityText = trimmedReality,
                ),
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            realityAnchorRepository.deleteEntry(id)
        }
    }
}
