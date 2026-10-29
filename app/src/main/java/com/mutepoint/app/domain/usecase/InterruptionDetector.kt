package com.mutepoint.app.domain.usecase

import com.mutepoint.app.domain.model.InterruptionEvent
import java.time.Instant

class InterruptionDetector(
    private val interruptionThresholdMs: Long = DEFAULT_INTERRUPTION_THRESHOLD_MS,
    private val energyThresholdDb: Float = DEFAULT_ENERGY_THRESHOLD_DB,
) {
    private var overlapStartMs: Long? = null
    private var peakUserDb: Float = 0f
    private var peakSecondaryDb: Float = 0f

    fun onFrame(
        timestampMs: Long,
        userVadActive: Boolean,
        secondaryVadActive: Boolean,
        userEnergyDb: Float,
        secondaryEnergyDb: Float,
    ): InterruptionEvent? {
        val overlapping =
            userVadActive &&
                secondaryVadActive &&
                secondaryEnergyDb > energyThresholdDb

        if (!overlapping) {
            reset()
            return null
        }

        if (overlapStartMs == null) {
            overlapStartMs = timestampMs
            peakUserDb = userEnergyDb
            peakSecondaryDb = secondaryEnergyDb
        } else {
            peakUserDb = maxOf(peakUserDb, userEnergyDb)
            peakSecondaryDb = maxOf(peakSecondaryDb, secondaryEnergyDb)
        }

        val elapsed = timestampMs - (overlapStartMs ?: timestampMs)
        if (elapsed < interruptionThresholdMs) return null

        val event = InterruptionEvent(
            timestamp = Instant.ofEpochMilli(overlapStartMs ?: timestampMs),
            durationMillis = elapsed,
            speakerVolumeDb = peakUserDb,
            interruptionVolumeDb = peakSecondaryDb,
        )
        reset()
        return event
    }

    fun reset() {
        overlapStartMs = null
        peakUserDb = 0f
        peakSecondaryDb = 0f
    }

    companion object {
        const val DEFAULT_INTERRUPTION_THRESHOLD_MS = 800L
        const val DEFAULT_ENERGY_THRESHOLD_DB = -30f
    }
}
