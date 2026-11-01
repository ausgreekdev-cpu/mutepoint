package com.mutepoint.app.presentation.fakecall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.service.FakeCallManager

@Composable
fun FakeCallScreen() {
    val context = LocalContext.current
    var callerName by remember { mutableStateOf("Mom") }
    var delayMinutes by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fake Call") }) },
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
                "Schedule a fake call to escape",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = callerName,
                onValueChange = { callerName = it },
                label = { Text("Caller Name") },
            )
            Spacer(Modifier.height(16.dp))

            Text("Delay: ${delayMinutes.toInt()} minute(s)")
            Slider(
                value = delayMinutes,
                onValueChange = { delayMinutes = it },
                valueRange = 1f..30f,
                steps = 28,
            )
            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                FakeCallManager.scheduleCall(context, delayMinutes.toInt(), callerName)
            }) {
                Text("Schedule Fake Call")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "A fake incoming call will appear at the scheduled time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}