package com.mutepoint.app.core.service

import android.content.Context
import android.content.Intent
import com.mutepoint.app.core.location.LocationManager
import com.mutepoint.app.domain.model.EmergencyContact
import com.mutepoint.app.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SosManagerTest {

    private class FakeContactsRepository(
        private val contacts: List<EmergencyContact> = emptyList(),
    ) : ContactsRepository {
        override fun observeContacts(): Flow<List<EmergencyContact>> = flowOf(contacts)
        override suspend fun getContacts(): List<EmergencyContact> = contacts
        override suspend fun getPrimary(): EmergencyContact? = contacts.firstOrNull { it.isPrimary }
        override suspend fun addContact(contact: EmergencyContact) {}
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun removeContact(contact: EmergencyContact) {}
    }

    @Test
    fun `sendSos with no contacts returns failure`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val sosManager = SosManager(
            context = context,
            contactsRepository = FakeContactsRepository(emptyList()),
            locationManager = LocationManager(context),
        )
        val result = sosManager.sendSos()
        assertEquals(0, result.sentCount)
        assertFalse(result.hasLocation)
        assertTrue(result.message.contains("No emergency contacts"))
    }

    @Test
    fun `sendSosViaIntent creates sms intent`() {
        val context = RuntimeEnvironment.getApplication()
        val sosManager = SosManager(
            context = context,
            contactsRepository = FakeContactsRepository(),
            locationManager = LocationManager(context),
        )
        val intent = sosManager.sendSosViaIntent("Help")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("Help", intent.getStringExtra("sms_body"))
    }

    @Test
    fun `callEmergency creates call intent`() {
        val context = RuntimeEnvironment.getApplication()
        val sosManager = SosManager(
            context = context,
            contactsRepository = FakeContactsRepository(),
            locationManager = LocationManager(context),
        )
        val intent = sosManager.callEmergency("911")
        assertEquals(Intent.ACTION_CALL, intent.action)
        assertEquals("tel:911", intent.data.toString())
    }
}
