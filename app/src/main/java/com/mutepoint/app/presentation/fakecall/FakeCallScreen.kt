package com.mutepoint.app.presentation.fakecall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.service.FakeCallScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FakeCallEntryPoint {
    fun fakeCallScheduler(): FakeCallScheduler
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scheduler = remember {
        EntryPointAccessors.fromApplication(context, FakeCallEntryPoint::class.java).fakeCallScheduler()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fake Call") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Schedule a fake incoming call", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "A notification will appear after the delay to help you exit a situation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                scheduler.scheduleFakeCall(15)
                scope.launch { snackbarHostState.showSnackbar("Fake call scheduled in 15 seconds") }
            }, modifier = Modifier.fillMaxWidth()) { Text("15 seconds") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                scheduler.scheduleFakeCall(30)
                scope.launch { snackbarHostState.showSnackbar("Fake call scheduled in 30 seconds") }
            }, modifier = Modifier.fillMaxWidth()) { Text("30 seconds") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                scheduler.scheduleFakeCall(60)
                scope.launch { snackbarHostState.showSnackbar("Fake call scheduled in 60 seconds") }
            }, modifier = Modifier.fillMaxWidth()) { Text("60 seconds") }
        }
    }
}
