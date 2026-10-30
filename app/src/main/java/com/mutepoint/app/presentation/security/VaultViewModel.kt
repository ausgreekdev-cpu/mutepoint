package com.mutepoint.app.presentation.security

import androidx.lifecycle.ViewModel
import com.mutepoint.app.core.security.RelockDecider
import com.mutepoint.app.core.security.RelockPolicy
import com.mutepoint.app.core.security.VaultSecurityStore
import com.mutepoint.app.core.security.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val store: VaultSecurityStore,
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _showOnboarding = MutableStateFlow(store.isFirstRun)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    val defaultPin: String? = store.defaultPin

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow(0L)
    val lockoutSeconds: StateFlow<Long> = _lockoutSeconds.asStateFlow()

    private val _flagSecure = MutableStateFlow(store.flagSecure)
    val flagSecure: StateFlow<Boolean> = _flagSecure.asStateFlow()

    private val _relockPolicy = MutableStateFlow(store.relockPolicy)
    val relockPolicy: StateFlow<RelockPolicy> = _relockPolicy.asStateFlow()

    private var backgroundedAt: Long? = null

    fun submitPin(pin: String) {
        if (pin.length < MIN_PIN_LENGTH) {
            _pinError.value = false
            return
        }
        when (val result = store.verifyPin(pin)) {
            is PinResult.Valid -> {
                _isUnlocked.value = true
                _pinError.value = false
                _lockoutSeconds.value = 0L
            }
            is PinResult.Invalid -> {
                _pinError.value = true
            }
            is PinResult.Locked -> {
                _lockoutSeconds.value = result.remainingSeconds
            }
        }
    }

    fun lock() {
        _isUnlocked.value = false
        _pinError.value = false
        backgroundedAt = null
    }

    fun dismissOnboarding() {
        store.markFirstRunComplete()
        _showOnboarding.value = false
    }

    fun onBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
        if (store.relockPolicy == RelockPolicy.IMMEDIATE) {
            lock()
        }
    }

    fun onForegrounded() {
        val at = backgroundedAt ?: return
        backgroundedAt = null
        if (RelockDecider.shouldLock(store.relockPolicy, at, System.currentTimeMillis())) {
            lock()
        }
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (store.verifyPin(currentPin) !is PinResult.Valid) return false
        store.changePin(newPin)
        return true
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        store.relockPolicy = policy
        _relockPolicy.value = policy
    }

    fun setFlagSecure(enabled: Boolean) {
        store.flagSecure = enabled
        _flagSecure.value = enabled
    }

    companion object {
        private const val MIN_PIN_LENGTH = 4
    }
}
