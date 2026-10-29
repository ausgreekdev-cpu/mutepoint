package com.mutepoint.app.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

    fun verifyPin(pin: String): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return PinHasher.verify(pin, decode(hash), decode(salt))
    }

    fun changePin(newPin: String) {
        val (hash, salt) = PinHasher.create(newPin)
        prefs.edit {
            putString(KEY_PIN_HASH, encode(hash))
            putString(KEY_PIN_SALT, encode(salt))
        }
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
        private const val DEFAULT_PIN = "7412"
    }
}
