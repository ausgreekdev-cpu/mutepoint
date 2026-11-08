package com.mutepoint.app.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.model.RealityAnchorEntry
import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.repository.AnalyticsRepository
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineItem(
    val id: String,
    val timestampMs: Long,
    val type: TimelineType,
    val title: String,
    val subtitle: String,
)

enum class TimelineType { SESSION, JOURNAL }

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val realityAnchorRepository: RealityAnchorRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<TimelineItem>>(emptyList())
    val items: StateFlow<List<TimelineItem>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                analyticsRepository.observeRecentSessions(50),
                realityAnchorRepository.observeEntries(),
            ) { sessions, entries ->
                buildTimeline(sessions, entries)
            }.collect { _items.value = it }
        }
    }

    private fun buildTimeline(
        sessions: List<SessionAnalytics>,
        entries: List<RealityAnchorEntry>,
    ): List<TimelineItem> {
        val sessionItems = sessions.map { session ->
            TimelineItem(
                id = "session_${session.sessionId}",
                timestampMs = session.startedAtMs,
                type = TimelineType.SESSION,
                title = "Session",
                subtitle = "${session.interruptionCount} interruption${if (session.interruptionCount == 1) "" else "s"} · ${session.speakingPercent.toInt()}% speaking",
            )
        }
        val journalItems = entries.map { entry ->
            TimelineItem(
                id = "journal_${entry.id}",
                timestampMs = entry.timestamp.toEpochMilli(),
                type = TimelineType.JOURNAL,
                title = "Journal Entry",
                subtitle = entry.triggerText.take(80).ifBlank { "Anchor recorded" },
            )
        }
        return (sessionItems + journalItems).sortedByDescending { it.timestampMs }
    }
}
