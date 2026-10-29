package com.mutepoint.app

import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mutepoint.app.presentation.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the security-critical vault flows: decoy lock screen, PIN
 * verification, relock-on-background, and FLAG_SECURE.
 *
 * Fresh installs seed the default PIN (7412), IMMEDIATE relock, and
 * screenshot blocking — these tests depend on those defaults.
 */
@RunWith(AndroidJUnit4::class)
class VaultSecurityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lockedStateShowsDecoy() {
        composeRule.onNodeWithText("My Notes & Ideas").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Note / Search").assertIsDisplayed()
        composeRule.onNodeWithText("Anchor this entry").assertDoesNotExist()
    }

    @Test
    fun wrongPinShowsDecoyError() {
        composeRule.onNode(hasSetTextAction()).performTextInput("0000")
        composeRule.onNodeWithText("Note not found.").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Note / Search").assertIsDisplayed()
    }

    @Test
    fun correctPinUnlocks() {
        composeRule.onNode(hasSetTextAction()).performTextInput("7412")
        composeRule.onNodeWithText("Anchor this entry").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Note / Search").assertDoesNotExist()
    }

    @Test
    fun backgroundRelocksImmediately() {
        composeRule.onNode(hasSetTextAction()).performTextInput("7412")
        composeRule.onNodeWithText("Anchor this entry").assertIsDisplayed()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.onNodeWithText("Quick Note / Search").assertIsDisplayed()
        composeRule.onNodeWithText("Anchor this entry").assertDoesNotExist()
    }

    @Test
    fun flagSecureEnabledByDefault() {
        // The window flag is applied asynchronously from the security store.
        var secure = false
        val deadline = System.currentTimeMillis() + 3_000
        while (!secure && System.currentTimeMillis() < deadline) {
            composeRule.activityRule.scenario.onActivity { activity ->
                secure = activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_SECURE != 0
            }
            if (!secure) Thread.sleep(100)
        }
        assertTrue("FLAG_SECURE should be set by default", secure)
    }
}
