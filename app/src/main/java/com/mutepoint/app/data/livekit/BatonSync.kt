package com.mutepoint.app.data.livekit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BATON_DATA_TOPIC = "baton"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
sealed interface BatonSyncMessage {
    @Serializable
    @SerialName("request")
    data class Request(
        val batonId: String,
        val identity: String,
        val ts: Long,
    ) : BatonSyncMessage

    @Serializable
    @SerialName("cancel")
    data class Cancel(
        val batonId: String,
        val identity: String,
    ) : BatonSyncMessage

    @Serializable
    @SerialName("grant")
    data class Grant(
        val winner: String,
        val expiresAtEpochMs: Long,
    ) : BatonSyncMessage

    @Serializable
    @SerialName("pass")
    data class Pass(
        val nextWinner: String? = null,
        val nextExpiresAtEpochMs: Long? = null,
    ) : BatonSyncMessage
}

fun BatonSyncMessage.encode(): ByteArray =
    json.encodeToString(BatonSyncMessage.serializer(), this).toByteArray()

fun decodeBatonMessage(data: ByteArray): BatonSyncMessage? =
    runCatching { json.decodeFromString(BatonSyncMessage.serializer(), data.decodeToString()) }.getOrNull()
