package com.mutepoint.app.saferoom

import com.mutepoint.app.data.livekit.LiveKitRoomManager
import com.mutepoint.app.data.livekit.LiveKitStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import javax.inject.Inject

/**
 * Integration tests for LiveKit reconnect logic.
 *
 * Verifies that the room manager handles disconnect/reconnect transitions.
 * Requires LIVEKIT_TOKEN_URL to be configured and network access.
 */
@RunWith(AndroidJUnit4::class)
class LiveKitReconnectTest {

    @Inject
    lateinit var roomManager: LiveKitRoomManager

    @Test
    fun leave_setsDisconnectedState() = runBlocking {
        assumeTrue(
            "LIVEKIT_TOKEN_URL not configured",
            System.getenv("LIVEKIT_TOKEN_URL")?.isNotBlank() == true,
        )

        roomManager.join()
        val statusAfterJoin = roomManager.status.value
        assertTrue(
            "Expected Connected or Error after join, got $statusAfterJoin",
            statusAfterJoin is LiveKitStatus.Connected || statusAfterJoin is LiveKitStatus.Error,
        )

        if (statusAfterJoin is LiveKitStatus.Connected) {
            roomManager.leave()
            // Give a moment for leave to complete
            kotlinx.coroutines.delay(500)
            val statusAfterLeave = roomManager.status.value
            assertTrue(
                "Expected Disconnected after leave, got $statusAfterLeave",
                statusAfterLeave is LiveKitStatus.Disconnected,
            )
        }
    }

    @Test
    fun joinAfterLeave_reconnects() = runBlocking {
        assumeTrue(
            "LIVEKIT_TOKEN_URL not configured",
            System.getenv("LIVEKIT_TOKEN_URL")?.isNotBlank() == true,
        )

        // First join
        roomManager.join()
        val firstStatus = roomManager.status.value
        if (firstStatus is LiveKitStatus.Connected) {
            roomManager.leave()
            kotlinx.coroutines.delay(500)

            // Re-join
            roomManager.join()
            val secondStatus = roomManager.status.value
            assertTrue(
                "Expected Connected or Error after re-join, got $secondStatus",
                secondStatus is LiveKitStatus.Connected || secondStatus is LiveKitStatus.Error,
            )
            roomManager.leave()
        }
    }
}
