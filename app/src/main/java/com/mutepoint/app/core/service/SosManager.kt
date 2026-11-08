package com.mutepoint.app.core.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.telephony.SmsManager
import com.mutepoint.app.core.location.LocationManager
import com.mutepoint.app.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val locationManager: LocationManager,
) {
    data class SosResult(
        val sentCount: Int,
        val hasLocation: Boolean,
        val message: String,
    )

    suspend fun sendSos(customMessage: String = "I need help. This is an emergency."): SosResult {
        val contacts = contactsRepository.getContacts()
        if (contacts.isEmpty()) {
            return SosResult(0, false, "No emergency contacts configured")
        }

        val location = try {
            locationManager.getCurrentLocation()
        } catch (e: Exception) {
            null
        }

        val locationText = if (location != null) {
            "\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            ""
        }

        val message = "$customMessage$locationText\nSent from Mute Point"

        var sentCount = 0
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        for (contact in contacts) {
            try {
                if (smsManager != null) {
                    smsManager.sendTextMessage(contact.phone, null, message, null, null)
                    sentCount++
                }
            } catch (e: Exception) {
            }
        }

        return SosResult(sentCount, location != null, message)
    }

    fun sendSosViaIntent(message: String = "I need help. Emergency."): Intent {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "smsto:".toUri()
            putExtra("sms_body", message)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun callEmergency(number: String = "911"): Intent {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:$number".toUri()
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
