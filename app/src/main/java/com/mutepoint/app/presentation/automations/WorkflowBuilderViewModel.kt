package com.mutepoint.app.presentation.automations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.automation.model.Action
import com.mutepoint.app.core.automation.model.Condition
import com.mutepoint.app.core.automation.model.Trigger
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WorkflowBuilderState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val name: String = "",
    val description: String = "",
    val triggerType: String = "Manual",
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action> = emptyList(),
    val actionCount: Int = 0,
)

@HiltViewModel
class WorkflowBuilderViewModel @Inject constructor(
    private val workflowRepository: WorkflowRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkflowBuilderState())
    val uiState: StateFlow<WorkflowBuilderState> = _uiState.asStateFlow()

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun nextStep() {
        val current = _uiState.value
        if (current.currentStep < current.totalSteps - 1) {
            _uiState.value = current.copy(currentStep = current.currentStep + 1)
        }
    }

    fun previousStep() {
        val current = _uiState.value
        if (current.currentStep > 0) {
            _uiState.value = current.copy(currentStep = current.currentStep - 1)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val workflow = Workflow(
                id = UUID.randomUUID().toString(),
                name = state.name,
                description = state.description,
                trigger = Trigger.Manual,
                conditions = state.conditions,
                actions = state.actions,
            )
            workflowRepository.saveWorkflow(workflow)
        }
    }
}
