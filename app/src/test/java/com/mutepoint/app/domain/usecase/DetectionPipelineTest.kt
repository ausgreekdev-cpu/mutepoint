package com.mutepoint.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionPipelineTest {

    private class FakeVad : SpeechVad {
        var nextProbability = 0f
        var resets = 0

        override fun processFrame(pcm: FloatArray): Float = nextProbability

        override fun isActive(probability: Float): Boolean = probability >= 0.5f

        override fun resetStates() {
            resets++
        }
    }

    private fun buildPipeline(
        vad: FakeVad,
        onFloor: () -> Boolean,
    ) = DetectionPipeline(
        vad = vad,
        isUserOnFloor = onFloor,
        frameDurationMs = 32L,
    )

    private fun loudFrame(): FloatArray = FloatArray(512) { 0.5f }

    @Test
    fun `speech shorter than overlap threshold produces no event`() {
        val vad = FakeVad().apply { nextProbability = 0.9f }
        val pipeline = buildPipeline(vad) { true }

        repeat(24) { index ->
            val event = pipeline.onFrame(loudFrame(), timestampMs = index * 32L)
            assertNull("frame $index should not trigger", event)
        }
    }

    @Test
    fun `sustained speech while user holds floor produces event`() {
        val vad = FakeVad().apply { nextProbability = 0.9f }
        val pipeline = buildPipeline(vad) { true }

        var event: com.mutepoint.app.domain.model.InterruptionEvent? = null
        repeat(30) { index ->
            val frameEvent = pipeline.onFrame(loudFrame(), timestampMs = index * 32L)
            if (frameEvent != null) event = frameEvent
        }

        assertNotNull("expected an interruption event", event)
        assertEquals(800L, event!!.durationMillis)
        assertTrue(event.speakerVolumeDb > -30f)
        assertTrue(event.interruptionVolumeDb > -30f)
    }

    @Test
    fun `sustained speech while user off floor produces no event`() {
        val vad = FakeVad().apply { nextProbability = 0.9f }
        val pipeline = buildPipeline(vad) { false }

        repeat(40) { index ->
            val event = pipeline.onFrame(loudFrame(), timestampMs = index * 32L)
            assertNull("frame $index should not trigger while off floor", event)
        }
    }

    @Test
    fun `speaking time accrues only while on floor and drains once`() {
        val vad = FakeVad()
        var onFloor = true
        val pipeline = buildPipeline(vad) { onFloor }

        repeat(10) { index ->
            pipeline.onFrame(loudFrame(), timestampMs = index * 32L)
        }
        assertEquals(320L, pipeline.drainSpeakingTime())
        assertEquals(0L, pipeline.drainSpeakingTime())

        onFloor = false
        repeat(10) { index ->
            pipeline.onFrame(loudFrame(), timestampMs = 1_000L + index * 32L)
        }
        assertEquals(0L, pipeline.drainSpeakingTime())
    }

    @Test
    fun `reset clears vad state detector and speaking time`() {
        val vad = FakeVad().apply { nextProbability = 0.9f }
        val pipeline = buildPipeline(vad) { true }

        repeat(3) { index ->
            pipeline.onFrame(loudFrame(), timestampMs = index * 32L)
        }
        assertEquals(96L, pipeline.speakingTimeMs)

        pipeline.reset()

        assertEquals(0L, pipeline.speakingTimeMs)
        assertEquals(1, vad.resets)
    }

    @Test
    fun `rms of silence floors at -90db and loud frame is above energy threshold`() {
        assertEquals(DetectionPipeline.MIN_DB, DetectionPipeline.rmsDb(FloatArray(0)), 0.01f)
        assertEquals(DetectionPipeline.MIN_DB, DetectionPipeline.rmsDb(FloatArray(512)), 0.01f)
        val loud = DetectionPipeline.rmsDb(loudFrame())
        assertTrue("expected loud frame above -30dB but was $loud", loud > -30f)
    }
}
