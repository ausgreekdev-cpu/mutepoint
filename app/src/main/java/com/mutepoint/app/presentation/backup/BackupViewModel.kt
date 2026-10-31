package com.mutepoint.app.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import com.mutepoint.app.domain.repository.AnalyticsRepository
import com.mutepoint.app.presentation.journal.EncryptedExport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val realityAnchorRepository: RealityAnchorRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun exportData(passphrase: String) {
        if (passphrase.isBlank()) {
            _status.value = "Passphrase required"
            return
        }
        viewModelScope.launch {
            _status.value = "Exporting..."
            try {
                val entries = mutableListOf<JSONObject>()
                realityAnchorRepository.getAllEntries().collect { list ->
                    list.forEach { entry ->
                        val obj = JSONObject().apply {
                            put("id", entry.id)
                            put("triggerText", entry.triggerText)
                            put("realityText", entry.realityText)
                            put("timestamp", entry.timestamp.toEpochMilli())
                        }
                        entries.add(obj)
                    }
                }
                val json = JSONObject().apply {
                    put("version", 1)
                    put("entries", JSONArray(entries))
                }.toString()
                val encrypted = EncryptedExport.encrypt(json, passphrase)
                // In production, write to SAF file
                _status.value = "Export encrypted (${encrypted.length} chars)"
            } catch (e: Exception) {
                _status.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importData(passphrase: String) {
        if (passphrase.isBlank()) {
            _status.value = "Passphrase required"
            return
        }
        viewModelScope.launch {
            _status.value = "Importing..."
            // In production, read from SAF file
            _status.value = "Import ready (select file to decrypt)"
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
