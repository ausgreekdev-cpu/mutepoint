package com.mutepoint.app.presentation.security

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val TRIPLE_TAP_WINDOW_MS = 400L

fun Modifier.panicGesture(onPanicTriggered: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        var lastTapTime = 0L
        var tapCount = 0
        detectTapGestures(
            onTap = {
                val now = System.currentTimeMillis()
                tapCount = if (now - lastTapTime < TRIPLE_TAP_WINDOW_MS) {
                    tapCount + 1
                } else {
                    1
                }
                lastTapTime = now
                if (tapCount >= 3) {
                    tapCount = 0
                    onPanicTriggered()
                }
            },
        )
    }
