package com.mutepoint.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelockDeciderTest {

    private val now = 1_000_000L

    @Test
    fun `immediate policy locks as soon as backgrounded`() {
        assertTrue(RelockDecider.shouldLock(RelockPolicy.IMMEDIATE, backgroundedAt = now - 1, now = now))
        assertTrue(RelockDecider.shouldLock(RelockPolicy.IMMEDIATE, backgroundedAt = now, now = now))
    }

    @Test
    fun `grace policy keeps vault open within the window`() {
        assertFalse(
            RelockDecider.shouldLock(
                RelockPolicy.AFTER_30_SECONDS,
                backgroundedAt = now - 10_000L,
                now = now,
            ),
        )
        assertTrue(
            RelockDecider.shouldLock(
                RelockPolicy.AFTER_30_SECONDS,
                backgroundedAt = now - 30_000L,
                now = now,
            ),
        )
    }

    @Test
    fun `two minute policy uses its own delay`() {
        assertFalse(
            RelockDecider.shouldLock(
                RelockPolicy.AFTER_2_MINUTES,
                backgroundedAt = now - 30_000L,
                now = now,
            ),
        )
        assertTrue(
            RelockDecider.shouldLock(
                RelockPolicy.AFTER_2_MINUTES,
                backgroundedAt = now - 120_000L,
                now = now,
            ),
        )
    }

    @Test
    fun `app-close-only never relocks while process alive`() {
        assertFalse(
            RelockDecider.shouldLock(
                RelockPolicy.ONLY_WHEN_APP_CLOSES,
                backgroundedAt = now - 999_999L,
                now = now,
            ),
        )
    }

    @Test
    fun `foreground never relocks regardless of policy`() {
        RelockPolicy.entries.forEach { policy ->
            assertFalse(RelockDecider.shouldLock(policy, backgroundedAt = null, now = now))
        }
    }

    @Test
    fun `fromDelay maps stored values and falls back to immediate`() {
        assertTrue(RelockPolicy.fromDelay(0L) == RelockPolicy.IMMEDIATE)
        assertTrue(RelockPolicy.fromDelay(30_000L) == RelockPolicy.AFTER_30_SECONDS)
        assertTrue(RelockPolicy.fromDelay(120_000L) == RelockPolicy.AFTER_2_MINUTES)
        assertTrue(RelockPolicy.fromDelay(-1L) == RelockPolicy.ONLY_WHEN_APP_CLOSES)
        assertTrue(RelockPolicy.fromDelay(12345L) == RelockPolicy.IMMEDIATE)
    }
}
