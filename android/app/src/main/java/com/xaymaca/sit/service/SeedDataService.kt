package com.xaymaca.sit.service

import android.content.Context
import com.xaymaca.sit.data.repository.ContactRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG ONLY — loads test_contacts.csv from assets and imports it via LinkedInCSVParser.
 * Gated by BuildConfig.DEBUG so it is compiled out of release builds.
 */
@Singleton
class SeedDataService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val linkedInCSVParser: LinkedInCSVParser
) {
    suspend fun seedTestContacts(): Int = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open("test_contacts.csv")
        val contacts = linkedInCSVParser.parse(inputStream)
        contacts.forEach { contactRepository.insertContact(it) }
        contacts.size
    }
}
