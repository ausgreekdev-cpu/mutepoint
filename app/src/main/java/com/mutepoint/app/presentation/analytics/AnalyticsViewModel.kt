package com.mutepoint.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class InsightsFilter(val days: Long?, val label: String) {
    LAST_7(7L, "7d"),
    LAST_30(30L, "30d"),
    ALL(null, "All"),
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val filter: InsightsFilter = InsightsFilter.LAST_7,
    val speakingPercent: Float = 0f,
    val timeCutOffMillis: Long = 0L,
    val totalInterruptions: Int = 0,
    val sessions: List<SessionAnalytics> = emptyList(),
    val interruptionBars: List<Int> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && sessions.isEmpty()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(InsightsFilter.LAST_7)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _openSession = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openSession: SharedFlow<String> = _openSession.asSharedFlow()

    init {
        filter
            .flatMapLatest { f ->
                analyticsRepository.observeRecentSessions(MAX_SESSIONS).map { all ->
                    val cutoffMs = f.days?.let { days -> System.currentTimeMillis() - days * DAY_MS }
                    val filtered = if (cutoffMs == null) {
                        all
                    } else {
                        all.filter { it.startedAtMs >= cutoffMs }
                    }
                    f to filtered
                }
            }
            .onEach { (f, filtered) -> _uiState.value = buildState(f, filtered) }
            .launchIn(viewModelScope)
    }

    fun setFilter(newFilter: InsightsFilter) {
        filter.value = newFilter
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch { _openSession.emit(sessionId) }
    }

    private fun buildState(
        f: InsightsFilter,
        sessions: List<SessionAnalytics>,
    ): AnalyticsUiState {
        if (sessions.isEmpty()) {
            return AnalyticsUiState(isLoading = false, filter = f)
        }
        return AnalyticsUiState(
            isLoading = false,
            filter = f,
            speakingPercent = sessions.map { it.speakingPercent }.average().toFloat(),
            timeCutOffMillis = sessions.sumOf { it.timeCutOffMillis },
            totalInterruptions = sessions.sumOf { it.interruptionCount },
            sessions = sessions,
            interruptionBars = sessions.reversed().map { it.interruptionCount },
        )
    }

    companion object {
        private const val MAX_SESSIONS = 100
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
