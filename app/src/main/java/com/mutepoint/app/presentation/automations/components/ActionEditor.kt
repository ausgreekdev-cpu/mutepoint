package com.mutepoint.app.presentation.automations.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.automation.model.Action

@Composable
fun ActionEditor(
    actions: List<Action>,
    onAdd: (Action) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column {
        actions.forEachIndexed { index, action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(
                    action::class.simpleName ?: "Action",
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onRemove(index) }) { Text("Remove") }
            }
        }
        Button(onClick = { onAdd(Action.StartMonitoring) }) { Text("Add Action") }
    }
}
