package com.mutepoint.app.domain.usecase

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InterruptionDetectorTest {

    private val detector = InterruptionDetector(
        interruptionThresholdMs = 800L,
        energyThresholdDb = -30f,
    )

    @Test
    fun `no event when overlap shorter than threshold`() {
        assertNull(
            detector.onFrame(
                timestampMs = 0L,
                userVadActive = true,
                secondaryVadActive = true,
                userEnergyDb = -20f,
                secondaryEnergyDb = -10f,
            ),
        )
        assertNull(
            detector.onFrame(
                timestampMs = 400L,
                userVadActive = true,
                secondaryVadActive = true,
                userEnergyDb = -20f,
                secondaryEnergyDb = -10f,
            ),
        )
    }

    @Test
    fun `event fires once overlap reaches threshold`() {
        detector.onFrame(0L, true, true, -20f, -10f)
        detector.onFrame(400L, true, true, -20f, -12f)
        val event = detector.onFrame(800L, true, true, -22f, -8f)

        assertNotNull(event)
        event!!
        assert(event.durationMillis >= 800L)
        assert(event.speakerVolumeDb == -20f)
        assert(event.interruptionVolumeDb == -8f)
    }

    @Test
    fun `timer resets when secondary speaker stops`() {
        detector.onFrame(0L, true, true, -20f, -10f)
        detector.onFrame(400L, true, false, -20f, -60f)
        detector.onFrame(800L, true, true, -20f, -10f)

        assertNull(
            detector.onFrame(1000L, true, true, -20f, -10f),
        )
    }

    @Test
    fun `secondary energy below threshold is ignored`() {
        detector.onFrame(0L, true, true, -20f, -40f)
        detector.onFrame(500L, true, true, -20f, -35f)
        assertNull(
            detector.onFrame(900L, true, true, -20f, -32f),
        )
    }

    @Test
    fun `no event when user is silent`() {
        detector.onFrame(0L, false, true, -60f, -10f)
        detector.onFrame(500L, false, true, -60f, -10f)
        assertNull(
            detector.onFrame(900L, false, true, -60f, -10f),
        )
    }
}
