package com.mutepoint.app.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri?.let { viewModel.exportToUri(context, it, passphrase) }
        showExportDialog = false
        passphrase = ""
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromUri(context, it, passphrase) }
        showImportDialog = false
        passphrase = ""
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false; passphrase = "" },
            title = { Text("Set Export Passphrase") },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    exportLauncher.launch("mutepoint-backup.json")
                }) {
                    Text("Choose File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false; passphrase = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; passphrase = "" },
            title = { Text("Enter Import Passphrase") },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text("Choose File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; passphrase = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Backup & Restore") }) },
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
                "Export or import your data",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Data is exported as encrypted JSON. You'll need a passphrase to decrypt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(24.dp))

            Button(onClick = { showExportDialog = true }) {
                Text("Export Data")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showImportDialog = true }) {
                Text("Import Data")
            }

            if (status != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    status!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}