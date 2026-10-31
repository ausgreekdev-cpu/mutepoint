package com.mutepoint.app.core.automation.engine

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple navigation event bus for workflow-triggered navigation.
 */
object NavigationBus {
    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    fun navigateTo(screen: String) {
        _navigationEvents.tryEmit(screen)
    }
}
