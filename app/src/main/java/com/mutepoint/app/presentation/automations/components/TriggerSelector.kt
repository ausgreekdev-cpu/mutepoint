package com.mutepoint.app.presentation.automations.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.automation.model.Trigger

@Composable
fun TriggerSelector(
    selected: Trigger,
    onSelect: (Trigger) -> Unit,
) {
    Column {
        TriggerOption("Manual", Trigger.Manual, selected, onSelect)
        TriggerOption("On Event", Trigger.OnEvent(com.mutepoint.app.core.automation.model.EventType.ROOM_JOINED), selected, onSelect)
        TriggerOption("On Schedule", Trigger.OnSchedule(com.mutepoint.app.core.automation.model.Schedule(9, 0)), selected, onSelect)
    }
}

@Composable
private fun TriggerOption(
    label: String,
    trigger: Trigger,
    selected: Trigger,
    onSelect: (Trigger) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        RadioButton(
            selected = selected::class == trigger::class,
            onClick = { onSelect(trigger) },
        )
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
