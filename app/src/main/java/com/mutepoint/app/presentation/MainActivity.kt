package com.mutepoint.app.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
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

        setContent {
            MutePointTheme {
                val isUnlocked by vaultViewModel.isUnlocked.collectAsStateWithLifecycle()
                val pinError by vaultViewModel.pinError.collectAsStateWithLifecycle()
                val lockoutSeconds by vaultViewModel.lockoutSeconds.collectAsStateWithLifecycle()
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
            }
        }
    }
}
