package com.mutepoint.app.presentation.automations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.automation.model.Workflow
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AutomationsViewModel @Inject constructor(
    private val workflowRepository: WorkflowRepository,
) : ViewModel() {

    val workflows: StateFlow<List<Workflow>> = workflowRepository.observeWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { workflowRepository.toggleEnabled(id, enabled) }
    }

    fun deleteWorkflow(id: String) {
        viewModelScope.launch { workflowRepository.deleteWorkflow(id) }
    }
}
