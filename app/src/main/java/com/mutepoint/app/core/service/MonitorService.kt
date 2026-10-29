package com.mutepoint.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mutepoint.app.BuildConfig
import com.mutepoint.app.R
import com.mutepoint.app.core.audio.AudioRecorderManager
import com.mutepoint.app.core.audio.VadConfig
import com.mutepoint.app.core.audio.VadEngine
import com.mutepoint.app.core.di.ApplicationScope
import com.mutepoint.app.domain.model.BatonState
import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.repository.AnalyticsRepository
import com.mutepoint.app.domain.repository.RoomRepository
import com.mutepoint.app.domain.usecase.DetectionPipeline
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MonitorService : LifecycleService() {

    @Inject lateinit var recorder: AudioRecorderManager
    @Inject lateinit var vadEngine: VadEngine
    @Inject lateinit var vadConfig: VadConfig
    @Inject lateinit var roomRepository: RoomRepository
    @Inject lateinit var analyticsRepository: AnalyticsRepository
    @Inject lateinit var monitorController: MonitorController

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    private var pipeline: DetectionPipeline? = null
    private var sessionId: String? = null
    private var batonJob: Job? = null
    private var speakingFlushJob: Job? = null

    @Volatile
    private var userOnFloor = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopAndKill()
            ACTION_SIMULATE_INTERRUPTION -> handleSimulatedInterruption(intent)
            else -> return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        if (pipeline != null) return
        startForegroundWithNotification()

        val id = UUID.randomUUID().toString()
        sessionId = id
        appScope.launch {
            analyticsRepository.startSession(id, System.currentTimeMillis())
        }

        val pipe = DetectionPipeline(
            vad = vadEngine,
            isUserOnFloor = { userOnFloor },
            frameDurationMs = 1_000L * vadConfig.frameSizeSamples / vadConfig.sampleRateHz,
        )
        pipeline = pipe

        batonJob = roomRepository.batonState
            .onEach { state -> userOnFloor = state is BatonState.HoldingBaton }
            .launchIn(lifecycleScope)

        speakingFlushJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                delay(SPEAKING_FLUSH_INTERVAL_MS)
                val sid = sessionId ?: break
                val pending = pipe.drainSpeakingTime()
                if (pending > 0) {
                    analyticsRepository.recordSpeakingTime(sid, pending)
                }
            }
        }

        recorder.start { frame ->
            val event = pipe.onFrame(frame, System.currentTimeMillis())
            if (event != null) {
                handleInterruption(event)
            }
        }
    }

    private fun handleInterruption(event: InterruptionEvent) {
        monitorController.publishInterruption(event)
        notifyInterruption()
        val sid = sessionId ?: return
        appScope.launch {
            analyticsRepository.recordInterruption(sid, event)
        }
    }

    private fun handleSimulatedInterruption(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val sid = sessionId ?: return
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 800L)
        val speakerDb = intent.getFloatExtra(EXTRA_SPEAKER_DB, -18f)
        val otherDb = intent.getFloatExtra(EXTRA_OTHER_DB, -11f)
        val speakingMs = intent.getLongExtra(EXTRA_SPEAKING_MS, 0L)
        if (speakingMs > 0) {
            appScope.launch {
                analyticsRepository.recordSpeakingTime(sid, speakingMs)
            }
        }
        handleInterruption(
            InterruptionEvent(
                timestamp = Instant.now(),
                durationMillis = durationMs,
                speakerVolumeDb = speakerDb,
                interruptionVolumeDb = otherDb,
            ),
        )
    }

    private fun stopAndKill() {
        val wasActive = pipeline != null
        recorder.stop()
        val pendingSpeaking = pipeline?.drainSpeakingTime() ?: 0L
        pipeline?.reset()
        pipeline = null
        batonJob?.cancel()
        batonJob = null
        speakingFlushJob?.cancel()
        speakingFlushJob = null
        userOnFloor = false

        val sid = sessionId
        sessionId = null
        if (wasActive && sid != null) {
            // App scope, not lifecycleScope: stopSelf() below destroys the
            // service and cancels lifecycleScope before these writes run.
            appScope.launch {
                if (pendingSpeaking > 0) {
                    analyticsRepository.recordSpeakingTime(sid, pendingSpeaking)
                }
                analyticsRepository.endSession(sid, System.currentTimeMillis())
            }
        }
        monitorController.onServiceStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        recorder.stop()
        vadEngine.release()
        pipeline?.reset()
        pipeline = null
        batonJob?.cancel()
        speakingFlushJob?.cancel()
        monitorController.onServiceStopped()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val notification = buildBaseNotification().build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyInterruption() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            INTERRUPTION_NOTIFICATION_ID,
            buildBaseNotification()
                .setContentTitle(getString(R.string.monitor_interruption_title))
                .setContentText(getString(R.string.monitor_interruption_text))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun buildBaseNotification(): NotificationCompat.Builder {
        createChannel()
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, getString(R.string.monitor_notification_stop), stopIntent)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.monitor_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.mutepoint.app.action.START_MONITORING"
        const val ACTION_STOP = "com.mutepoint.app.action.STOP_MONITORING"
        const val ACTION_SIMULATE_INTERRUPTION = "com.mutepoint.app.action.SIMULATE_INTERRUPTION"
        const val EXTRA_DURATION_MS = "durationMs"
        const val EXTRA_SPEAKER_DB = "speakerDb"
        const val EXTRA_OTHER_DB = "otherDb"
        const val EXTRA_SPEAKING_MS = "speakingMs"
        private const val CHANNEL_ID = "mute_point_monitor"
        private const val NOTIFICATION_ID = 41
        private const val INTERRUPTION_NOTIFICATION_ID = 42
        private const val SPEAKING_FLUSH_INTERVAL_MS = 5_000L
    }
}
