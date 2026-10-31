package com.mutepoint.app.presentation.security

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class VaultContainerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unlockedState_showsMainContent() {
        composeTestRule.setContent {
            VaultContainer(
                isUnlocked = true,
                pinError = false,
                lockoutSeconds = 0,
                onPinSubmit = {},
                onPanic = {},
                onUnlockAuthenticated = { Text("Main Content") },
            )
        }
        composeTestRule.onNodeWithText("Main Content").assertExists()
    }

    @Test
    fun lockedState_showsPinField() {
        composeTestRule.setContent {
            VaultContainer(
                isUnlocked = false,
                pinError = false,
                lockoutSeconds = 0,
                onPinSubmit = {},
                onPanic = {},
                onUnlockAuthenticated = {},
            )
        }
        composeTestRule.onNodeWithText("Quick Note / Search").assertExists()
    }

    @Test
    fun pinError_showsErrorMessage() {
        composeTestRule.setContent {
            VaultContainer(
                isUnlocked = false,
                pinError = true,
                lockoutSeconds = 0,
                onPinSubmit = {},
                onPanic = {},
                onUnlockAuthenticated = {},
            )
        }
        composeTestRule.onNodeWithText("Note not found.").assertExists()
    }

    @Test
    fun lockout_disablesPinField() {
        composeTestRule.setContent {
            VaultContainer(
                isUnlocked = false,
                pinError = false,
                lockoutSeconds = 30,
                onPinSubmit = {},
                onPanic = {},
                onUnlockAuthenticated = {},
            )
        }
        composeTestRule.onNodeWithText("Quick Note / Search").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Too many attempts. Try again in 30s.").assertExists()
    }

    @Test
    fun lockoutZero_enablesPinField() {
        composeTestRule.setContent {
            VaultContainer(
                isUnlocked = false,
                pinError = false,
                lockoutSeconds = 0,
                onPinSubmit = {},
                onPanic = {},
                onUnlockAuthenticated = {},
            )
        }
        composeTestRule.onNodeWithText("Quick Note / Search").assertIsEnabled()
    }
}
