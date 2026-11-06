package com.mutepoint.app.presentation.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    viewModel: SosViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onManageContacts: () -> Unit = {},
) {
    val contactsCount by viewModel.contactsCount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] == true
        if (smsGranted) {
            viewModel.sendSos()
        } else {
            viewModel.showPermissionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Emergency") },
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
            Icon(
                Icons.Default.Warning,
                contentDescription = "Emergency",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Emergency SOS",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (contactsCount == 0) {
                    "No emergency contacts configured. Add trusted contacts to send SOS alerts with your location."
                } else {
                    "$contactsCount emergency contact${if (contactsCount == 1) "" else "s"} will be notified via SMS"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val smsOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                    val locOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    if (smsOk) {
                        viewModel.sendSos()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                },
                enabled = contactsCount > 0 && !uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    if (uiState.isSending) "Sending..." else "SEND SOS NOW",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onManageContacts) {
                Text("Manage Emergency Contacts")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                try {
                    val intent = viewModel.getSosIntent()
                    context.startActivity(intent)
                } catch (e: Exception) {
                }
            }) {
                Text("Send via SMS App")
            }
        }

        if (uiState.showResult && uiState.lastResult != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("SOS Status") },
                text = {
                    Text(
                        "Sent to ${uiState.lastResult!!.sentCount} contact(s)\n" +
                            if (uiState.lastResult!!.hasLocation) "Location included\n" else "Location not available\n" +
                                uiState.lastResult!!.message.take(100) + if (uiState.lastResult!!.message.length > 100) "..." else "",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissResult() }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}
