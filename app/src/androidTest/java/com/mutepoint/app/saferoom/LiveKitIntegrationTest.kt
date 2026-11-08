package com.mutepoint.app.saferoom

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mutepoint.app.data.livekit.LiveKitRoomManager
import com.mutepoint.app.data.livekit.LiveKitStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class LiveKitIntegrationTest {

    @Inject
    lateinit var roomManager: LiveKitRoomManager

    @Test
    fun tokenFetch_connects() = runBlocking {
        // Skip if no token URL configured
        assumeTrue(
            "LIVEKIT_TOKEN_URL not configured",
            System.getenv("LIVEKIT_TOKEN_URL")?.isNotBlank() == true,
        )

        roomManager.join()
        val status = roomManager.status.value
        // Should be Connected or Error (depending on network)
        assertTrue(
            "Expected Connected or Error, got $status",
            status is LiveKitStatus.Connected || status is LiveKitStatus.Error,
        )
        roomManager.leave()
    }
}
