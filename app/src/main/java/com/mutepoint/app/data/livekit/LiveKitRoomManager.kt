package com.mutepoint.app.data.livekit

import android.content.Context
import com.mutepoint.app.BuildConfig
import androidx.core.content.edit
import com.mutepoint.app.core.di.ApplicationScope
import com.mutepoint.app.domain.model.BatonState
import com.mutepoint.app.domain.usecase.BatonRequest
import com.mutepoint.app.domain.usecase.BatonSyncCommand
import com.mutepoint.app.domain.usecase.BatonSyncReducer
import com.mutepoint.app.domain.usecase.BatonSyncResult
import com.mutepoint.app.domain.usecase.BatonSyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TokenResponse(
    val token: String,
    val url: String,
    val room: String? = null,
)

/**
 * Owns the LiveKit Room for the buddy Safe Room (walkie model).
 *
 * Local mic publish is gated by baton state; remote audio plays automatically
 * once subscribed. Baton state is shared across participants over a reliable
 * data channel (topic [BATON_DATA_TOPIC]) using [BatonSyncReducer], so both
 * phones agree on who holds the floor and the turn deadline.
 */
@Singleton
class LiveKitRoomManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var room: Room? = null
    private var micPublishRequested = false
    private var myIdentity: String? = null

    private val batonMutex = Mutex()
    private val reducer = BatonSyncReducer()
    private var batonSyncState = BatonSyncState()
    private var myPendingRequest: BatonRequest? = null
    private var tickerJob: Job? = null

    private companion object {
        private const val KEY_IDENTITY = "identity"
    }

    private val _status = MutableStateFlow<LiveKitStatus>(LiveKitStatus.Disconnected)
    val status: StateFlow<LiveKitStatus> = _status.asStateFlow()

    private val _batonState = MutableStateFlow<BatonState>(BatonState.Idle)
    val batonState: StateFlow<BatonState> = _batonState.asStateFlow()

    private val _batonEvents = MutableSharedFlow<BatonSyncMessage>(
        extraBufferCapacity = 16,
    )
    val batonEvents = _batonEvents.asSharedFlow()

    init {
        appScope.launch {
            _batonEvents.collect { msg -> handleBatonMessage(msg) }
        }
    }

    suspend fun join() {
        mutex.withLock {
            val current = _status.value
            if (current is LiveKitStatus.Connecting || current is LiveKitStatus.Connected) return
            _status.value = LiveKitStatus.Connecting
            try {
                val identity = identity()
                myIdentity = identity
                val tokenResponse = withContext(Dispatchers.IO) { fetchToken(identity) }
                val lkRoom = LiveKit.create(appContext)
                appScope.launch {
                    lkRoom.events.events.collect { event -> handleRoomEvent(lkRoom, event) }
                }
                lkRoom.connect(tokenResponse.url, tokenResponse.token)
                room = lkRoom
                publishConnected(lkRoom)
                if (micPublishRequested) {
                    lkRoom.localParticipant.setMicrophoneEnabled(true)
                }
            } catch (e: Exception) {
                room = null
                _status.value = LiveKitStatus.Error(e.message ?: "join failed")
            }
        }
    }

    fun leave() {
        appScope.launch { mutex.withLock { teardownLocked() } }
    }

    fun setMicPublish(enabled: Boolean) {
        micPublishRequested = enabled
        val lkRoom = room ?: return
        appScope.launch {
            try {
                lkRoom.localParticipant.setMicrophoneEnabled(enabled)
            } catch (_: Exception) {
                // best-effort; the LIVE chip reflects connection, not track state
            }
        }
    }

    fun requestBaton() {
        appScope.launch {
            batonMutex.withLock {
                val me = myIdentity ?: return@withLock
                if (_status.value !is LiveKitStatus.Connected) return@withLock
                if (batonSyncState.holder == me || myPendingRequest != null) return@withLock
                val request = BatonRequest(
                    batonId = UUID.randomUUID().toString(),
                    identity = me,
                    ts = System.currentTimeMillis(),
                )
                myPendingRequest = request
                publishBaton(BatonSyncMessage.Request(request.batonId, request.identity, request.ts))
                val result = reducer.onRequest(
                    state = batonSyncState,
                    request = request,
                    nowMs = System.currentTimeMillis(),
                    turnMs = BatonSyncReducer.TURN_MS,
                )
                applyBatonResultLocked(me, result)
            }
        }
    }

    fun passBaton() {
        appScope.launch {
            batonMutex.withLock {
                val me = myIdentity ?: return@withLock
                if (batonSyncState.holder == me) {
                    val now = System.currentTimeMillis()
                    val next = batonSyncState.pending.firstOrNull()
                    val nextExpires = next?.let { now + BatonSyncReducer.TURN_MS }
                    applyBatonResultLocked(
                        me,
                        reducer.onPass(batonSyncState, next?.identity, nextExpires),
                    )
                    publishBaton(BatonSyncMessage.Pass(next?.identity, nextExpires))
                } else {
                    myPendingRequest?.let { request ->
                        applyBatonResultLocked(me, reducer.onCancel(batonSyncState, request.batonId))
                        publishBaton(BatonSyncMessage.Cancel(request.batonId, me))
                    }
                    myPendingRequest = null
                }
            }
        }
    }

    private suspend fun handleBatonMessage(msg: BatonSyncMessage) {
        batonMutex.withLock {
            val me = myIdentity ?: return
            val now = System.currentTimeMillis()
            val result: BatonSyncResult = when (msg) {
                is BatonSyncMessage.Request -> reducer.onRequest(
                    state = batonSyncState,
                    request = BatonRequest(msg.batonId, msg.identity, msg.ts),
                    nowMs = now,
                    turnMs = BatonSyncReducer.TURN_MS,
                )
                is BatonSyncMessage.Cancel -> reducer.onCancel(batonSyncState, msg.batonId)
                is BatonSyncMessage.Grant -> reducer.onGrant(batonSyncState, msg.winner, msg.expiresAtEpochMs)
                is BatonSyncMessage.Pass -> reducer.onPass(batonSyncState, msg.nextWinner, msg.nextExpiresAtEpochMs)
            }
            applyBatonResultLocked(me, result)
        }
    }

    private fun applyBatonResultLocked(myIdentity: String, result: BatonSyncResult) {
        batonSyncState = result.state
        if (myPendingRequest != null &&
            batonSyncState.pending.none { it.batonId == myPendingRequest?.batonId }
        ) {
            myPendingRequest = null
        }
        result.commands.forEach { command ->
            when (command) {
                is BatonSyncCommand.BroadcastGrant ->
                    publishBaton(BatonSyncMessage.Grant(command.winner, command.expiresAtEpochMs))
                is BatonSyncCommand.BroadcastPass ->
                    publishBaton(BatonSyncMessage.Pass(command.nextWinner, command.nextExpiresAtEpochMs))
            }
        }
        updateBatonStateUiLocked(myIdentity)
        if (batonSyncState.holder == myIdentity) {
            startTickerLocked()
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun updateBatonStateUiLocked(myIdentity: String) {
        val ui = reducer.uiStateFor(batonSyncState, myIdentity, System.currentTimeMillis())
        _batonState.value = if (ui is BatonState.Listening) {
            ui.copy(activeSpeakerName = displayNameFor(ui.activeSpeakerName))
        } else {
            ui
        }
    }

    private fun startTickerLocked() {
        tickerJob?.cancel()
        tickerJob = appScope.launch {
            while (isActive) {
                delay(500L)
                batonMutex.withLock {
                    val me = myIdentity ?: return@withLock
                    val result = reducer.onExpiry(
                        state = batonSyncState,
                        myIdentity = me,
                        nowMs = System.currentTimeMillis(),
                        turnMs = BatonSyncReducer.TURN_MS,
                    )
                    applyBatonResultLocked(me, result)
                }
            }
        }
    }

    private fun publishBaton(msg: BatonSyncMessage) {
        val lkRoom = room ?: return
        val bytes = msg.encode()
        appScope.launch {
            try {
                lkRoom.localParticipant.publishData(
                    bytes,
                    DataPublishReliability.RELIABLE,
                    BATON_DATA_TOPIC,
                    null,
                )
            } catch (_: Exception) {
                // best-effort; retry happens on next user action
            }
        }
    }

    private fun displayNameFor(identity: String): String {
        val remote = room?.remoteParticipants?.get(io.livekit.android.room.participant.Participant.Identity(identity))
        return remote?.name?.takeIf { it.isNotBlank() } ?: identity
    }

    private fun handleRoomEvent(lkRoom: Room, event: RoomEvent) {
        when (event) {
            is RoomEvent.Connected -> publishConnected(lkRoom)
            is RoomEvent.Disconnected -> {
                room = null
                _status.value = LiveKitStatus.Disconnected
                appScope.launch { resetBatonLocked() }
            }
            is RoomEvent.FailedToConnect -> {
                room = null
                _status.value = LiveKitStatus.Error("failed to connect")
                appScope.launch { resetBatonLocked() }
            }
            is RoomEvent.ParticipantConnected -> {
                publishConnected(lkRoom)
                appScope.launch {
                    batonMutex.withLock {
                        val st = batonSyncState
                        if (st.holder == myIdentity && st.expiresAtEpochMs != null) {
                            publishBaton(BatonSyncMessage.Grant(st.holder!!, st.expiresAtEpochMs!!))
                        }
                    }
                }
            }
            is RoomEvent.ParticipantDisconnected -> publishConnected(lkRoom)
            is RoomEvent.ActiveSpeakersChanged -> {
                val speakers = event.speakers
                    .filter { it != lkRoom.localParticipant }
                    .map {
                        it.name?.takeIf { n -> n.isNotBlank() }
                            ?: it.identity?.value
                            ?: "unknown"
                    }
                publishConnected(lkRoom, speakers)
            }
            is RoomEvent.DataReceived -> {
                if (event.topic == BATON_DATA_TOPIC) {
                    decodeBatonMessage(event.data)?.let { _batonEvents.tryEmit(it) }
                }
            }
            else -> Unit
        }
    }

    private fun publishConnected(lkRoom: Room, speakers: List<String>? = null) {
        val count = 1 + lkRoom.remoteParticipants.size
        val currentSpeakers = when (val s = _status.value) {
            is LiveKitStatus.Connected -> s.remoteSpeakers
            else -> emptyList()
        }
        _status.value = LiveKitStatus.Connected(
            participantCount = count,
            remoteSpeakers = speakers ?: currentSpeakers,
        )
    }

    private suspend fun fetchToken(identity: String): TokenResponse {
        val endpoint = BuildConfig.LIVEKIT_TOKEN_URL
        require(endpoint.isNotBlank() && !endpoint.contains("PLACEHOLDER")) {
            "token endpoint not configured (set LIVEKIT_TOKEN_URL in local.properties)"
        }
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Room-Code", BuildConfig.LIVEKIT_ROOM_CODE)
            val body =
                """{"room":"${BuildConfig.LIVEKIT_ROOM_NAME}","identity":"$identity"}"""
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("empty response ($code)")
            if (code !in 200..299) throw IOException("token server returned $code")
            return Json { ignoreUnknownKeys = true }.decodeFromString(text)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Stable-per-install anonymous identity. A random UUID (not ANDROID_ID)
     * avoids collisions between cloned devices and needs no hardware-ids
     * permission; the worker's regex allows it ([A-Za-z0-9_-]{1,64}).
     */
    private fun identity(): String {
        val prefs = appContext.getSharedPreferences("livekit_identity", Context.MODE_PRIVATE)
        return prefs.getString(KEY_IDENTITY, null) ?: run {
            val fresh = "user-${UUID.randomUUID().toString().replace("-", "").take(16)}"
            prefs.edit { putString(KEY_IDENTITY, fresh) }
            fresh
        }
    }

    private suspend fun resetBatonLocked() {
        batonMutex.withLock {
            tickerJob?.cancel()
            tickerJob = null
            batonSyncState = BatonSyncState()
            myPendingRequest = null
            _batonState.value = BatonState.Idle
        }
    }

    private suspend fun teardownLocked() {
        room?.let { r ->
            try {
                r.localParticipant.setMicrophoneEnabled(false)
            } catch (_: Exception) {
            }
            try {
                r.disconnect()
            } catch (_: Exception) {
            }
        }
        room = null
        micPublishRequested = false
        _status.value = LiveKitStatus.Disconnected
        resetBatonLocked()
    }
}
