package com.mutepoint.app.domain.model

import java.time.Instant

data class RealityAnchorEntry(
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val triggerText: String,
    val realityText: String,
)
