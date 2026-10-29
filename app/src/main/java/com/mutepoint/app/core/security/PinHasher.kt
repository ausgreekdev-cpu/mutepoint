package com.mutepoint.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 PIN hashing. The PIN only gates the in-app UI — the SQLCipher
 * passphrase is independently wrapped by AndroidKeystore.
 */
object PinHasher {

    private const val ALGORITHM = "SHA-256"

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(ALGORITHM)
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    fun create(pin: String): Pair<ByteArray, ByteArray> {
        val salt = newSalt()
        return hash(pin, salt) to salt
    }

    fun verify(pin: String, expectedHash: ByteArray, salt: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expectedHash)
}
