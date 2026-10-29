package com.mutepoint.app.core.security

enum class RelockPolicy(val delayMs: Long) {
    IMMEDIATE(0L),
    AFTER_30_SECONDS(30_000L),
    AFTER_2_MINUTES(120_000L),
    ONLY_WHEN_APP_CLOSES(-1L),
    ;

    companion object {
        fun fromDelay(delayMs: Long): RelockPolicy =
            entries.firstOrNull { it.delayMs == delayMs } ?: IMMEDIATE
    }
}

object RelockDecider {

    /**
     * @param backgroundedAt when the app last left the foreground, or null if
     *   it is currently in the foreground.
     * @return true when the vault must show the lock screen again.
     */
    fun shouldLock(
        policy: RelockPolicy,
        backgroundedAt: Long?,
        now: Long,
    ): Boolean {
        if (backgroundedAt == null) return false
        return when (policy) {
            RelockPolicy.IMMEDIATE -> true
            RelockPolicy.ONLY_WHEN_APP_CLOSES -> false
            RelockPolicy.AFTER_30_SECONDS,
            RelockPolicy.AFTER_2_MINUTES,
            -> now - backgroundedAt >= policy.delayMs
        }
    }
}
