package com.mutepoint.app.core.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.mutepoint.app.domain.usecase.SpeechVad
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: VadConfig,
) : SpeechVad {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var state: OnnxTensor? = null
    private val fallbackDetector = VadFallbackDetector()
    var useFallback: Boolean = false
        private set

    private val sampleRate: Long = config.sampleRateHz.toLong()
    private val frameSize: Int = config.frameSizeSamples

    init {
        initOrtEngine()
        if (ortSession == null) {
            useFallback = true
        }
    }

    private fun initOrtEngine() {
        runCatching {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open(config.modelAssetName).readBytes()
            ortSession = ortEnv?.createSession(modelBytes)
            resetStates()
        }
    }

    override fun resetStates() {
        val env = ortEnv ?: return
        state?.close()
        val emptyBuffer = FloatArray(STATE_SIZE)
        state = OnnxTensor.createTensor(env, FloatBuffer.wrap(emptyBuffer), STATE_SHAPE)
    }

    override fun processFrame(pcmFloatArray: FloatArray): Float {
        if (useFallback || ortSession == null) {
            val shorts = ShortArray(pcmFloatArray.size)
            for (i in pcmFloatArray.indices) {
                shorts[i] = (pcmFloatArray[i] * 32768f).toInt().toShort()
            }
            return if (fallbackDetector.detect(shorts)) config.speechThreshold else 0f
        }
        val env = ortEnv ?: run { useFallback = true; return 0f }
        val session = ortSession ?: run { useFallback = true; return 0f }
        if (pcmFloatArray.size != frameSize) return 0f

        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(pcmFloatArray),
            longArrayOf(1, frameSize.toLong()),
        )
        val srTensor = OnnxTensor.createTensor(env, longArrayOf(sampleRate))
        val inputs = mapOf(
            "input" to inputTensor,
            "sr" to srTensor,
            "state" to state,
        )

        return try {
            val results = session.run(inputs)
            val outputTensor = results[0] as OnnxTensor
            val prob = outputTensor.floatBuffer.get(0)

            state?.close()
            state = results[1] as OnnxTensor
            inputTensor.close()
            srTensor.close()

            prob
        } catch (e: Exception) {
            inputTensor.close()
            srTensor.close()
            useFallback = true
            0f
        }
    }

    override fun isActive(probability: Float): Boolean = probability >= config.speechThreshold

    fun release() {
        state?.close()
        ortSession?.close()
        ortEnv?.close()
        state = null
        ortSession = null
        ortEnv = null
    }

    companion object {
        private val STATE_SHAPE = longArrayOf(2, 1, 128)
        private const val STATE_SIZE = 2 * 1 * 128
    }
}
