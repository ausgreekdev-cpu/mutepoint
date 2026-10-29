package com.mutepoint.app.presentation.security

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mutepoint.app.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun VaultContainer(
    isUnlocked: Boolean,
    pinError: Boolean,
    lockoutSeconds: Long,
    onPinSubmit: (String) -> Unit,
    onUnlockAuthenticated: @Composable () -> Unit,
    onPanic: () -> Unit,
) {
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(isUnlocked) {
        pinInput = ""
    }

    Crossfade(targetState = isUnlocked, label = "VaultTransition") { unlocked ->
        if (unlocked) {
            onUnlockAuthenticated()
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.decoy_app_name)) },
                        modifier = Modifier.panicGesture(onPanicTriggered = onPanic),
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val isLockedOut = lockoutSeconds > 0

                    OutlinedTextField(
                        value = if (isLockedOut) "" else pinInput,
                        onValueChange = { input ->
                            if (!isLockedOut) {
                                pinInput = input
                                onPinSubmit(input)
                            }
                        },
                        label = { Text(if (isLockedOut) "Locked" else "Quick Note / Search") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLockedOut,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (isLockedOut) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Too many attempts. Try again in ${lockoutSeconds}s.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else if (pinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.decoy_note_not_found),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.decoy_empty_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
