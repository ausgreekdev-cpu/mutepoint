package com.mutepoint.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    fun applyRetention(days: Int) {
        if (days <= 0) return // "forever" — no cleanup
        viewModelScope.launch {
            val cutoffMs = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            analyticsRepository.deleteOlderThan(cutoffMs)
        }
    }
}
