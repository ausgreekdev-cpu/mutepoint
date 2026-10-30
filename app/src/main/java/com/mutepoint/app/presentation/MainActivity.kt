package com.mutepoint.app.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mutepoint.app.core.ui.theme.MutePointTheme
import com.mutepoint.app.presentation.security.VaultContainer
import com.mutepoint.app.presentation.security.VaultViewModel
import com.mutepoint.app.presentation.security.triggerEmergencyExit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vaultViewModel: VaultViewModel by viewModels()

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "mutepoint" && uri.host == "room") {
            // Navigate to Safe Rooms tab — handled by NavGraph via state
            // For now, just store the room name for later navigation
            android.util.Log.d("MainActivity", "Deep link: ${uri.path}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                vaultViewModel.onBackgrounded()
            }

            override fun onStart(owner: LifecycleOwner) {
                vaultViewModel.onForegrounded()
            }
        })

        lifecycleScope.launch {
            vaultViewModel.flagSecure.collect { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        // Handle deep links
        handleDeepLink(intent)

        setContent {
            MutePointTheme {
                val isUnlocked by vaultViewModel.isUnlocked.collectAsStateWithLifecycle()
                val pinError by vaultViewModel.pinError.collectAsStateWithLifecycle()
                val lockoutSeconds by vaultViewModel.lockoutSeconds.collectAsStateWithLifecycle()
                val showOnboarding by vaultViewModel.showOnboarding.collectAsStateWithLifecycle()
                val defaultPin by remember { mutableStateOf(vaultViewModel.defaultPin) }

                VaultContainer(
                    isUnlocked = isUnlocked,
                    pinError = pinError,
                    lockoutSeconds = lockoutSeconds,
                    onPinSubmit = vaultViewModel::submitPin,
                    onPanic = { triggerEmergencyExit(this) },
                    onUnlockAuthenticated = {
                        MainAppScaffold()
                    },
                )

                // Onboarding dialog
                if (showOnboarding && isUnlocked) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { vaultViewModel.dismissOnboarding() },
                        title = { Text("Welcome to Mute Point") },
                        text = {
                            Text("Your default PIN is: $defaultPin\n\nPlease change it in Settings for security.")
                        },
                        confirmButton = {
                            TextButton(onClick = { vaultViewModel.dismissOnboarding() }) {
                                Text("Got it")
                            }
                        },
                    )
                }
            }
        }
    }
}
