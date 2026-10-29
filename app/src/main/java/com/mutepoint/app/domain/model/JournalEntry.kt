package com.mutepoint.app.domain.model

import java.time.Instant

data class JournalEntry(
    val id: Long = 0,
    val title: String,
    val durationMillis: Long,
    val filePath: String,
    val createdAt: Instant = Instant.now(),
)
