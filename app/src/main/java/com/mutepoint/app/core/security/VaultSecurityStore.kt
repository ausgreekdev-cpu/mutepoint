package com.mutepoint.app.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a PIN verification attempt. [Locked] includes remaining seconds.
 */
sealed class PinResult {
    data object Valid : PinResult()
    data object Invalid : PinResult()
    data class Locked(val remainingSeconds: Long) : PinResult()
}

@Singleton
class VaultSecurityStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_PIN_HASH)) {
            val (hash, salt) = PinHasher.create(DEFAULT_PIN)
            prefs.edit {
                putString(KEY_PIN_HASH, encode(hash))
                putString(KEY_PIN_SALT, encode(salt))
            }
        }
    }

    /**
     * Verifies a PIN attempt. Implements exponential backoff after repeated
     * failures to resist brute-force. Returns a [PinResult] indicating success,
     * failure, or lockout (with remaining seconds).
     */
    fun verifyPin(pin: String): PinResult {
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            return PinResult.Locked((lockedUntil - now) / 1000)
        }

        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return PinResult.Invalid
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return PinResult.Invalid
        val valid = PinHasher.verify(pin, decode(hash), decode(salt))

        if (valid) {
            // Reset failure counter on success
            prefs.edit { remove(KEY_FAILED_ATTEMPTS) }
            return PinResult.Valid
        }

        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Exponential backoff: 30s, 60s, 120s, capped at 300s
            val backoff = minOf(30L * (1 shl (attempts - MAX_FAILED_ATTEMPTS)), MAX_LOCKOUT_SECONDS)
            prefs.edit {
                putInt(KEY_FAILED_ATTEMPTS, attempts)
                putLong(KEY_LOCKED_UNTIL, now + backoff * 1000)
            }
            return PinResult.Locked(backoff)
        } else {
            prefs.edit { putInt(KEY_FAILED_ATTEMPTS, attempts) }
            return PinResult.Invalid
        }
    }

    fun changePin(newPin: String) {
        val (hash, salt) = PinHasher.create(newPin)
        prefs.edit {
            putString(KEY_PIN_HASH, encode(hash))
            putString(KEY_PIN_SALT, encode(salt))
        }
    }

    val isLocked: Boolean
        get() = System.currentTimeMillis() < prefs.getLong(KEY_LOCKED_UNTIL, 0L)

    val lockoutRemainingSeconds: Long
        get() {
            val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
            val now = System.currentTimeMillis()
            return if (lockedUntil > now) (lockedUntil - now) / 1000 else 0L
        }

    var relockPolicy: RelockPolicy
        get() = RelockPolicy.fromDelay(prefs.getLong(KEY_RELOCK_DELAY, RelockPolicy.IMMEDIATE.delayMs))
        set(value) {
            prefs.edit { putLong(KEY_RELOCK_DELAY, value.delayMs) }
        }

    var flagSecure: Boolean
        get() = prefs.getBoolean(KEY_FLAG_SECURE, true)
        set(value) {
            prefs.edit { putBoolean(KEY_FLAG_SECURE, value) }
        }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    companion object {
        private const val PREFS_NAME = "vault_security"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_RELOCK_DELAY = "relock_delay_ms"
        private const val KEY_FLAG_SECURE = "flag_secure"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until"
        private const val DEFAULT_PIN = "7412"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val MAX_LOCKOUT_SECONDS = 300L
    }
}
