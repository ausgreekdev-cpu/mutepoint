package com.mutepoint.app.data.livekit

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses token server response`() {
        val payload = """
            {"token":"eyJhbGciOiJIUzI1NiJ9.abc","url":"wss://example.livekit.cloud","room":"buddy"}
        """.trimIndent()
        val parsed = json.decodeFromString<TokenResponse>(payload)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.abc", parsed.token)
        assertEquals("wss://example.livekit.cloud", parsed.url)
        assertEquals("buddy", parsed.room)
    }

    @Test
    fun `room field is optional`() {
        val payload = """{"token":"t","url":"wss://x"}"""
        val parsed = json.decodeFromString<TokenResponse>(payload)
        assertNull(parsed.room)
    }

    @Test
    fun `ignores unknown fields from future server versions`() {
        val payload = """{"token":"t","url":"wss://x","room":"buddy","ttl":"15m","extra":true}"""
        val parsed = json.decodeFromString<TokenResponse>(payload)
        assertEquals("t", parsed.token)
    }
}
