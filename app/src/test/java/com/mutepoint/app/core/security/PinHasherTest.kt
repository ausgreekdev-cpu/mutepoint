package com.mutepoint.app.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `verify accepts the created pin`() {
        val (hash, salt) = PinHasher.create("7412")
        assertTrue(PinHasher.verify("7412", hash, salt))
    }

    @Test
    fun `verify rejects a wrong pin`() {
        val (hash, salt) = PinHasher.create("7412")
        assertFalse(PinHasher.verify("0000", hash, salt))
        assertFalse(PinHasher.verify("", hash, salt))
        assertFalse(PinHasher.verify("741", hash, salt))
    }

    @Test
    fun `same pin with different salts produces different hashes`() {
        val (hashA, saltA) = PinHasher.create("7412")
        val (hashB, saltB) = PinHasher.create("7412")
        assertFalse(saltA.contentEquals(saltB))
        assertFalse(hashA.contentEquals(hashB))
        assertTrue(PinHasher.verify("7412", hashA, saltA))
        assertTrue(PinHasher.verify("7412", hashB, saltB))
    }

    @Test
    fun `create returns hash consistent with hash()`() {
        val (hash, salt) = PinHasher.create("9911")
        assertArrayEquals(PinHasher.hash("9911", salt), hash)
    }
}
