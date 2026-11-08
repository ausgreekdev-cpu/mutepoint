package com.mutepoint.app.core.service

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FakeCallSchedulerTest {

    @Test
    fun `scheduleFakeCall does not crash`() {
        val context = RuntimeEnvironment.getApplication()
        val scheduler = FakeCallScheduler(context)
        scheduler.scheduleFakeCall(15, "Test Caller")
        scheduler.scheduleFakeCall(30)
    }
}
