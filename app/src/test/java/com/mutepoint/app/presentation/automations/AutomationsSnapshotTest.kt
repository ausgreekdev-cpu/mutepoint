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
class AutomationsSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun automationsScreen_empty() {
        composeTestRule.setContent {
            MaterialTheme {
                AutomationsScreen(
                    viewModel = androidx.lifecycle.ViewModelProvider(
                        androidx.lifecycle.ViewModelStore(),
                    ).get(AutomationsViewModel::class.java),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
