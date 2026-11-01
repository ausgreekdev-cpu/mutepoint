package com.mutepoint.app.core.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mutepoint.app.core.location.LocationManager
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val locationManager: LocationManager,
) {
    suspend fun sendSos() {
        val contacts = contactsRepository.getContacts()
        if (contacts.isEmpty()) return

        val location = locationManager.getCurrentLocation()
        val locationText = location?.let { locationManager.formatLocationUrl(it) } ?: ""

        val message = buildString {
            append("SOS - I need help!")
            if (locationText.isNotEmpty()) {
                append("\nMy location: $locationText")
            }
        }

        // Send SMS to all contacts
        val smsUri = Uri.parse("smsto:${contacts.joinToString(";") { it.phone }}")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}