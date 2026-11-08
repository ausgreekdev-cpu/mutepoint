package com.mutepoint.app.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckInManagerTest {

    @Test
    fun `schedule and cancel check-in updates state`() {
        val context = RuntimeEnvironment.getApplication()
        val manager = CheckInManager(context)

        assertFalse(manager.isActive())

        val dueTime = manager.scheduleCheckIn(15)
        assertTrue(manager.isActive())
        assertTrue(dueTime > System.currentTimeMillis())
        assertTrue(manager.getDueTime() > 0L)

        manager.cancelCheckIn()
        assertFalse(manager.isActive())
        assertEquals(0L, manager.getDueTime())
    }
}
