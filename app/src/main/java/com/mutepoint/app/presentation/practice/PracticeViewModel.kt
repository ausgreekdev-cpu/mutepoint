package com.mutepoint.app.presentation.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.data.repository.PracticeRepositoryImpl
import com.mutepoint.app.domain.model.PracticeCard
import com.mutepoint.app.domain.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    seedSource: PracticeRepositoryImpl,
) : ViewModel() {

    val cards: StateFlow<List<PracticeCard>> = practiceRepository.observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            seedSource.ensureSeedCards()
        }
    }
}
