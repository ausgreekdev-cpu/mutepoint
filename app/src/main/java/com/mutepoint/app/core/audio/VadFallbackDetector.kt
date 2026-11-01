package com.mutepoint.app.core.audio

import kotlin.math.abs

/**
 * Simple energy-based VAD fallback when ONNX model fails to load.
 */
class VadFallbackDetector {
    private var energyThreshold = 500.0

    fun detect(samples: ShortArray): Boolean {
        if (samples.isEmpty()) return false
        var energy = 0.0
        for (sample in samples) {
            energy += (sample.toDouble() * sample.toDouble())
        }
        energy = energy / samples.size
        return energy > energyThreshold
    }

    fun updateThreshold(threshold: Double) {
        energyThreshold = threshold
    }
}