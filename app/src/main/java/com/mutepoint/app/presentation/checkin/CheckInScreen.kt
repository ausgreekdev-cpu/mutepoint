package com.mutepoint.app.presentation.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in Timer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
            if (state.isActive) {
                val mins = (state.remainingMs / 60000).toInt()
                val secs = ((state.remainingMs % 60000) / 1000).toInt()
                Text(
                    "Check-in active",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    String.format(java.util.Locale.getDefault(), "%02d:%02d remaining", mins, secs),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.checkInNow() }, modifier = Modifier.fillMaxWidth()) {
                    Text("I'm Safe (Check In)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.cancelCheckIn() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel Timer")
                }
            } else {
                Text(
                    "Set a check-in timer",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "If you don't check in before time runs out, the app will prompt you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.startCheckIn(15) }) { Text("15 min") }
                    OutlinedButton(onClick = { viewModel.startCheckIn(30) }) { Text("30 min") }
                    OutlinedButton(onClick = { viewModel.startCheckIn(60) }) { Text("60 min") }
                }
            }
        }
    }
}
