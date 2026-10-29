package com.mutepoint.app.core.security

import android.content.Context
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurePassphraseProvider {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "mute_point_db_master_key"
    private const val PREFS_NAME = "secure_prefs"
    private const val TAG_CIPHERTEXT = "db_passphrase_ciphertext"
    private const val TAG_IV = "db_passphrase_iv"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedCipher = prefs.getString(TAG_CIPHERTEXT, null)
        val storedIv = prefs.getString(TAG_IV, null)
        if (storedCipher != null && storedIv != null) {
            return decrypt(
                Base64.decode(storedCipher, Base64.NO_WRAP),
                Base64.decode(storedIv, Base64.NO_WRAP),
            )
        }

        val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val (ciphertext, iv) = encrypt(passphrase)
        prefs.edit {
            putString(TAG_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(TAG_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
        }
        return passphrase
    }

    private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plain)
        return ciphertext to cipher.iv
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
