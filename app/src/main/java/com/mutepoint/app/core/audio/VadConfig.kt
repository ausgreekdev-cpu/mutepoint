package com.mutepoint.app.core.audio

data class VadConfig(
    val sampleRateHz: Int = 16_000,
    val frameSizeSamples: Int = 512,
    val speechThreshold: Float = 0.5f,
    val modelAssetName: String = "silero_vad.onnx",
)
