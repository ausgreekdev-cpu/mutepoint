package com.mutepoint.app.presentation.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SosScreen(
    viewModel: SosViewModel = hiltViewModel(),
) {
    val contactsCount by viewModel.contactsCount.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Emergency SOS") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Emergency Alert",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This will send your location to $contactsCount emergency contact(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.sendSos() },
                modifier = Modifier.size(200.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            ) {
                Text(
                    "SOS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Only use in case of emergency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}