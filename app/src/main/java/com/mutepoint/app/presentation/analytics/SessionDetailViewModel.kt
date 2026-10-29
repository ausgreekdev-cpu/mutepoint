package com.mutepoint.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SessionAnalytics?>(null)
    val session: StateFlow<SessionAnalytics?> = _session.asStateFlow()

    private var observeJob: Job? = null

    fun load(sessionId: String) {
        if (observeJob != null) return
        observeJob = analyticsRepository.observeSession(sessionId)
            .onEach { _session.value = it }
            .launchIn(viewModelScope)
    }
}
