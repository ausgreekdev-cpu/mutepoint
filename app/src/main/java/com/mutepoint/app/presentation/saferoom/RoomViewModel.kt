package com.mutepoint.app.presentation.saferoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.service.MonitorController
import com.mutepoint.app.data.livekit.LiveKitRoomManager
import com.mutepoint.app.data.livekit.LiveKitStatus
import com.mutepoint.app.domain.model.BatonState
import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val monitorController: MonitorController,
    private val liveKitRoomManager: LiveKitRoomManager,
) : ViewModel() {

    val liveStatus: StateFlow<LiveKitStatus> = liveKitRoomManager.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveKitStatus.Disconnected)

    val batonState: StateFlow<BatonState> = combine(
        roomRepository.batonState,
        liveKitRoomManager.batonState,
        liveStatus,
    ) { local, synced, status ->
        if (status is LiveKitStatus.Connected) synced else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatonState.Idle)

    val isMonitoring: StateFlow<Boolean> = monitorController.isMonitoring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lastInterruption = MutableStateFlow<InterruptionEvent?>(null)
    val lastInterruption: StateFlow<InterruptionEvent?> = _lastInterruption.asStateFlow()

    private var tickerJob: Job? = null
    private var bannerClearJob: Job? = null

    init {
        monitorController.lastInterruption
            .onEach { event ->
                _lastInterruption.value = event
                bannerClearJob?.cancel()
                bannerClearJob = viewModelScope.launch {
                    delay(BANNER_DURATION_MS)
                    _lastInterruption.value = null
                }
            }
            .launchIn(viewModelScope)

        batonState
            .onEach { state ->
                liveKitRoomManager.setMicPublish(state is BatonState.HoldingBaton)
            }
            .launchIn(viewModelScope)
    }

    fun toggleMonitoring() {
        if (isMonitoring.value) {
            monitorController.stop()
        } else {
            monitorController.start()
        }
    }

    fun joinRoom() {
        viewModelScope.launch { liveKitRoomManager.join() }
    }

    fun leaveRoom() {
        liveKitRoomManager.leave()
    }

    fun requestBaton() {
        if (liveStatus.value is LiveKitStatus.Connected) {
            liveKitRoomManager.requestBaton()
            return
        }
        viewModelScope.launch {
            roomRepository.requestBaton()
            delay(GRANT_DELAY_MS)
            grantBatonAndStartTicker()
        }
    }

    fun passBaton() {
        tickerJob?.cancel()
        tickerJob = null
        if (liveStatus.value is LiveKitStatus.Connected) {
            liveKitRoomManager.passBaton()
            return
        }
        viewModelScope.launch {
            when (batonState.value) {
                is BatonState.InQueue -> roomRepository.leaveQueue()
                else -> roomRepository.passBaton()
            }
        }
    }

    private fun grantBatonAndStartTicker() {
        viewModelScope.launch {
            roomRepository.grantBaton(timeSeconds = TURN_SECONDS)
            tickerJob?.cancel()
            tickerJob = viewModelScope.launch {
                repeat(TURN_SECONDS) {
                    delay(1_000L)
                    roomRepository.tick()
                }
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        bannerClearJob?.cancel()
        liveKitRoomManager.leave()
    }

    companion object {
        private const val GRANT_DELAY_MS = 2_000L
        private const val TURN_SECONDS = 30
        private const val BANNER_DURATION_MS = 5_000L
    }
}
