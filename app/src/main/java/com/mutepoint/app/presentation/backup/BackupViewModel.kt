package com.mutepoint.app.presentation.backup

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.first
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

    fun exportToUri(context: Context, uri: Uri, passphrase: String) {
        if (passphrase.isBlank()) {
            _status.value = "Passphrase required"
            return
        }
        viewModelScope.launch {
            _status.value = "Exporting..."
            try {
                val entries = mutableListOf<JSONObject>()
                val list = realityAnchorRepository.getAllEntries().first()
                list.forEach { entry ->
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("triggerText", entry.triggerText)
                        put("realityText", entry.realityText)
                        put("timestamp", entry.timestamp.toEpochMilli())
                    }
                    entries.add(obj)
                }
                val json = JSONObject().apply {
                    put("version", 1)
                    put("entries", JSONArray(entries))
                }.toString()
                val encrypted = EncryptedExport.encrypt(json, passphrase)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(encrypted.toByteArray(Charsets.UTF_8))
                }
                _status.value = "Export complete (${entries.size} entries)"
            } catch (e: Exception) {
                _status.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri, passphrase: String) {
        if (passphrase.isBlank()) {
            _status.value = "Passphrase required"
            return
        }
        viewModelScope.launch {
            _status.value = "Importing..."
            try {
                val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: throw Exception("Cannot read file")
                val json = EncryptedExport.decrypt(encrypted, passphrase)
                val obj = JSONObject(json)
                val entries = obj.getJSONArray("entries")
                _status.value = "Import ready (${entries.length()} entries) - restore requires merge logic"
            } catch (e: Exception) {
                _status.value = "Import failed: ${e.message}"
            }
        }
    }

    fun exportData(passphrase: String) {
        // Keep for backward compatibility
    }

    fun importData(passphrase: String) {
        // Keep for backward compatibility
    }

    fun clearStatus() {
        _status.value = null
    }
}
