package com.mutepoint.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mutepoint.app.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveKitJoinSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun joinRoomReachesConnectedState() {
        unlockAndOpenSafeRoom()
        composeRule.onNodeWithText("Join room").performClick()
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText("Leave room").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Leave room").assertIsDisplayed()
    }

    /**
     * Device A of the two-device buddy test: raises hand first, holds the
     * floor, passes to the queued device B, then confirms B took over.
     */
    @Test
    fun buddyHolderFlow() {
        unlockAndOpenSafeRoom()
        composeRule.onNodeWithText("Join room").performClick()
        waitUntilText("LIVE • 2")

        composeRule.onNodeWithText("Raise Hand to Speak").performClick()
        waitUntilText("YOU ARE SPEAKING")

        // Hold the floor while device B joins, queues and waits for the pass.
        Thread.sleep(30_000)

        composeRule.onNodeWithText("Pass the Baton Early").performClick()
        // B auto-granted the baton — A now sees the remote holder.
        waitUntilText("Has the floor")
    }

    /**
     * Device B of the two-device buddy test: joins while A holds, queues,
     * then is auto-granted the baton when A passes.
     */
    @Test
    fun buddyQueuedFlow() {
        unlockAndOpenSafeRoom()
        composeRule.onNodeWithText("Join room").performClick()
        waitUntilText("LIVE • 2")

        // Wait until A holds the floor, then queue behind A.
        waitUntilText("Has the floor")
        composeRule.onNodeWithText("Raise Hand to Speak").performClick()
        waitUntilText("In Queue")

        // A passes → B is auto-granted the floor.
        waitUntilText("YOU ARE SPEAKING")
    }

    private fun unlockAndOpenSafeRoom() {
        composeRule.onNode(hasSetTextAction()).performTextInput("7412")
        composeRule.onNodeWithText("Safe Rooms").performClick()
    }

    private fun waitUntilText(text: String) {
        composeRule.waitUntil(timeoutMillis = 90_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
