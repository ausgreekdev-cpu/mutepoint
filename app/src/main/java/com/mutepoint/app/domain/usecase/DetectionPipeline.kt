package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.InterruptionEvent
import kotlin.math.log10

class DetectionPipeline(
    private val vad: SpeechVad,
    private val detector: InterruptionDetector = InterruptionDetector(),
    private val isUserOnFloor: () -> Boolean,
    private val frameDurationMs: Long = DEFAULT_FRAME_DURATION_MS,
) {

    var speakingTimeMs: Long = 0L
        private set

    fun onFrame(pcm: FloatArray, timestampMs: Long): InterruptionEvent? {
        val userOnFloor = isUserOnFloor()
        if (userOnFloor) speakingTimeMs += frameDurationMs

        val probability = vad.processFrame(pcm)
        val secondaryActive = vad.isActive(probability)
        val energyDb = rmsDb(pcm)

        return detector.onFrame(
            timestampMs = timestampMs,
            userVadActive = userOnFloor,
            secondaryVadActive = secondaryActive,
            userEnergyDb = energyDb,
            secondaryEnergyDb = energyDb,
        )
    }

    fun drainSpeakingTime(): Long {
        val pending = speakingTimeMs
        speakingTimeMs = 0L
        return pending
    }

    fun reset() {
        vad.resetStates()
        detector.reset()
        speakingTimeMs = 0L
    }

    companion object {
        const val DEFAULT_FRAME_DURATION_MS = 32L
        const val MIN_DB = -90f

        fun rmsDb(frame: FloatArray): Float {
            if (frame.isEmpty()) return MIN_DB
            var sumSquares = 0.0
            for (sample in frame) {
                sumSquares += (sample * sample).toDouble()
            }
            val meanSquares = sumSquares / frame.size
            if (meanSquares <= 0.0) return MIN_DB
            return (10.0 * log10(meanSquares)).toFloat().coerceAtLeast(MIN_DB)
        }
    }
}
