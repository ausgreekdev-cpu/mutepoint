package com.mutepoint.app.core.analytics

import android.content.Context
import androidx.core.content.edit

/**
 * Manages user consent for analytics/telemetry.
 * All tracking is gated behind explicit user consent.
 */
object ConsentManager {
    private const val PREFS_NAME = "consent_prefs"
    private const val KEY_ANALYTICS_CONSENT = "analytics_consent"
    private const val KEY_CONSENT_ASKED = "consent_asked"

    fun hasConsented(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ANALYTICS_CONSENT, false)

    fun hasBeenAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONSENT_ASKED, false)

    fun setConsent(context: Context, consented: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ANALYTICS_CONSENT, consented)
            putBoolean(KEY_CONSENT_ASKED, true)
        }
    }
}
