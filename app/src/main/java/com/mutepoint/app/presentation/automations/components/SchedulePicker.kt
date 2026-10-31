package com.mutepoint.app.presentation.automations.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.automation.model.Schedule

@Composable
fun SchedulePicker(
    schedule: Schedule,
    onScheduleChange: (Schedule) -> Unit,
) {
    var hour by remember { mutableStateOf(schedule.hour.toString()) }
    var minute by remember { mutableStateOf(schedule.minute.toString().padStart(2, '0')) }

    Column {
        Text("Schedule", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = hour,
                onValueChange = { hour = it },
                label = { Text("Hour (0-23)") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = minute,
                onValueChange = { minute = it },
                label = { Text("Minute (0-59)") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
