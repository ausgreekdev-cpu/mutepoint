package com.mutepoint.app.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.repository.AnalyticsRepository
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TimelineEvent(
    val id: String,
    val timestamp: Long,
    val title: String,
    val description: String,
    val type: String,
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    realityAnchorRepository: RealityAnchorRepository,
) : ViewModel() {

    private val _events = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val events: StateFlow<List<TimelineEvent>> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                analyticsRepository.observeSessions(),
                realityAnchorRepository.getAllEntries(),
            ) { sessions, entries ->
                val timelineEvents = mutableListOf<TimelineEvent>()
                sessions.forEach { session ->
                    timelineEvents.add(
                        TimelineEvent(
                            id = session.sessionId,
                            timestamp = session.startedAtMs,
                            title = "Session",
                            description = "Speaking: ${session.speakingTimeMs / 1000}s, Interruptions: ${session.interruptionCount}",
                            type = "session",
                        ),
                    )
                }
                entries.forEach { entry ->
                    timelineEvents.add(
                        TimelineEvent(
                            id = entry.id.toString(),
                            timestamp = entry.timestamp.toEpochMilli(),
                            title = "Journal Entry",
                            description = "Trigger: ${entry.triggerText.take(50)}...",
                            type = "journal",
                        ),
                    )
                }
                timelineEvents.sortedByDescending { it.timestamp }
            }.collect { _events.value = it }
        }
    }
}