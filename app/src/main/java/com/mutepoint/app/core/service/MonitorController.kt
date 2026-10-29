package com.mutepoint.app.core.service

import android.content.Context
import android.content.Intent
import com.mutepoint.app.domain.model.InterruptionEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class MonitorController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastInterruption = MutableSharedFlow<InterruptionEvent>(extraBufferCapacity = 1)
    val lastInterruption: SharedFlow<InterruptionEvent> = _lastInterruption.asSharedFlow()

    fun start() {
        if (_isMonitoring.value) return
        _isMonitoring.value = true
        val intent = Intent(context, MonitorService::class.java).apply {
            action = MonitorService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stop() {
        _isMonitoring.value = false
        val intent = Intent(context, MonitorService::class.java).apply {
            action = MonitorService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
            .onFailure { context.stopService(Intent(context, MonitorService::class.java)) }
    }

    fun onServiceStopped() {
        _isMonitoring.value = false
    }

    fun publishInterruption(event: InterruptionEvent) {
        _lastInterruption.tryEmit(event)
    }

    companion object {
        fun stopFromAnywhere(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = MonitorService.ACTION_STOP
            }
            runCatching { context.startService(intent) }
                .onFailure { context.stopService(Intent(context, MonitorService::class.java)) }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonitorEntryPoint {
    fun monitorController(): MonitorController
}
