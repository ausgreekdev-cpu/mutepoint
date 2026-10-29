package com.mutepoint.app.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    private val config: VadConfig,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    val isCurrentlyRecording: Boolean
        get() = isRecording

    @SuppressLint("MissingPermission")
    fun start(onFrame: (FloatArray) -> Unit) {
        if (isRecording) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(config.sampleRateHz, channelConfig, audioFormat)
        val bufferSize = maxOf(minBuffer, config.frameSizeSamples * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRateHz,
            channelConfig,
            audioFormat,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }
        audioRecord = recorder
        isRecording = true

        recordingThread = Thread {
            val buffer = ShortArray(config.frameSizeSamples)
            recorder.startRecording()
            while (isRecording) {
                val read = recorder.read(buffer, 0, config.frameSizeSamples)
                if (read > 0) {
                    val floats = FloatArray(read) { i -> buffer[i] / 32768f }
                    onFrame(floats)
                } else if (read < 0) {
                    Thread.sleep(20L)
                }
            }
            runCatching { recorder.stop() }
            recorder.release()
        }.apply {
            name = "mute-point-audio-recorder"
            start()
        }
    }

    fun stop() {
        isRecording = false
        recordingThread?.join(500L)
        recordingThread = null
        audioRecord = null
    }
}
