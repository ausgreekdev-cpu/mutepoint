package com.mutepoint.app.presentation.automations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderScreen(
    viewModel: WorkflowBuilderViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Workflow") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Step indicator
            Text(
                "Step ${state.currentStep + 1} of ${state.totalSteps}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            when (state.currentStep) {
                0 -> {
                    // Name + Description
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Workflow Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                1 -> {
                    // Trigger selection
                    Text("Select Trigger", style = MaterialTheme.typography.titleMedium)
                    Text("Choose when this workflow should run.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    // Trigger options would be radio buttons here
                    Text("Manual (tap to run)", style = MaterialTheme.typography.bodyLarge)
                    Text("On Event (room joined, interruption, etc.)", style = MaterialTheme.typography.bodyLarge)
                    Text("On Schedule (daily, weekly)", style = MaterialTheme.typography.bodyLarge)
                }
                2 -> {
                    // Conditions (optional)
                    Text("Conditions (optional)", style = MaterialTheme.typography.titleMedium)
                    Text("All conditions must be true for the workflow to run.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                3 -> {
                    // Actions
                    Text("Actions", style = MaterialTheme.typography.titleMedium)
                    Text("What should happen when this workflow runs?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                4 -> {
                    // Preview
                    Text("Preview", style = MaterialTheme.typography.titleMedium)
                    Text("Name: ${state.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Trigger: ${state.triggerType}", style = MaterialTheme.typography.bodyLarge)
                    Text("Actions: ${state.actionCount}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (state.currentStep > 0) {
                    Button(onClick = viewModel::previousStep) { Text("Back") }
                } else {
                    Button(onClick = onCancel) { Text("Cancel") }
                }
                if (state.currentStep < state.totalSteps - 1) {
                    Button(onClick = viewModel::nextStep) { Text("Next") }
                } else {
                    Button(onClick = {
                        viewModel.save()
                        onSaved()
                    }) { Text("Save") }
                }
            }
        }
    }
}
