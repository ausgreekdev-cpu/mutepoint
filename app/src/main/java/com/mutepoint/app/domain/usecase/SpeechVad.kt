package com.mutepoint.app.domain.usecase

interface SpeechVad {
    fun processFrame(pcm: FloatArray): Float

    fun isActive(probability: Float): Boolean

    fun resetStates()
}
