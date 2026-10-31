package com.mutepoint.app.presentation.automations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.core.automation.repository.WorkflowExecution
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WorkflowExecutionHistoryViewModel @Inject constructor(
    private val workflowRepository: WorkflowRepository,
) : ViewModel() {

    private val _executions = MutableStateFlow<List<WorkflowExecution>>(emptyList())
    val executions: StateFlow<List<WorkflowExecution>> = _executions.asStateFlow()

    fun loadHistory(workflowId: String) {
        viewModelScope.launch {
            _executions.value = workflowRepository.getExecutionHistory(workflowId)
        }
    }
}
