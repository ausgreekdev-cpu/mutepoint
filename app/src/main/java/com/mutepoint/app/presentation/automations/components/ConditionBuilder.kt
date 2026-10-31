package com.mutepoint.app.presentation.automations.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mutepoint.app.core.automation.model.Condition

@Composable
fun ConditionBuilder(
    conditions: List<Condition>,
    onAdd: (Condition) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column {
        if (conditions.isEmpty()) {
            Text(
                "No conditions (workflow always runs)",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
            )
        }
        conditions.forEachIndexed { index, condition ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    condition::class.simpleName ?: "Condition",
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.TextButton(onClick = { onRemove(index) }) {
                    Text("Remove")
                }
            }
        }
        androidx.compose.material3.Button(onClick = { onAdd(Condition.HasInterruptions) }) {
            Text("Add Condition")
        }
    }
}
