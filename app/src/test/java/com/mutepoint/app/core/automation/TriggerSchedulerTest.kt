package com.mutepoint.app.core.automation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.mutepoint.app.core.automation.engine.TriggerScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TriggerSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun `start and stop do not crash`() {
        val scheduler = TriggerScheduler(context)
        scheduler.start()
        scheduler.stop()
    }
}
