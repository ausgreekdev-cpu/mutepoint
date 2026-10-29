package com.mutepoint.app.domain.model

import java.time.Instant

data class InterruptionEvent(
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val durationMillis: Long,
    val speakerVolumeDb: Float,
    val interruptionVolumeDb: Float,
)
