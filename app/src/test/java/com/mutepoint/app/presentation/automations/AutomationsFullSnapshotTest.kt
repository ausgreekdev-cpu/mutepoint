package com.mutepoint.app.presentation.automations

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AutomationsFullSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun workflowBuilderScreen_step0() {
        composeTestRule.setContent {
            MaterialTheme {
                WorkflowBuilderScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun workflowExecutionHistoryScreen_empty() {
        composeTestRule.setContent {
            MaterialTheme {
                WorkflowExecutionHistoryScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
