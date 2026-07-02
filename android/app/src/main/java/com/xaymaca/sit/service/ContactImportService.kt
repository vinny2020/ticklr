package com.xaymaca.sit.service

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.google.gson.Gson
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ImportSource
import com.xaymaca.sit.data.repository.ContactRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactImportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository
) {
    private val gson = Gson()

    /**
     * Reads all contacts from the device using ContactsContract.
     * Returns the number of contacts imported.
     * Caller is responsible for ensuring READ_CONTACTS permission is granted.
     */
    suspend fun importPhoneContacts(): Int {
        val contacts = withContext(Dispatchers.IO) { readDeviceContacts(context.contentResolver) }
        var importCount = 0
        contacts.forEach { contact ->
            contactRepository.insertContact(contact)
            importCount++
        }
        return importCount
    }

    private fun readDeviceContacts(resolver: ContentResolver): List<Contact> {
        val result = mutableListOf<Contact>()
        val contactMap = mutableMapOf<String, MutableMap<String, Any>>()

        // 1. Fetch display names
        val nameCursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )
        nameCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val displayName = cursor.getString(nameIdx) ?: ""
                contactMap[id] = mutableMapOf("displayName" to displayName)
            }
        }

        // 2. Fetch phone numbers
        val phoneCursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        phoneCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val number = cursor.getString(numIdx) ?: continue
                val entry = contactMap[id] ?: continue
                @Suppress("UNCHECKED_CAST")
                val phones = entry.getOrPut("phones") { mutableListOf<String>() } as MutableList<String>
                phones.add(number.trim())
            }
        }

        // 3. Fetch emails
        val emailCursor = resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null, null, null
        )
        emailCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val email = cursor.getString(emailIdx) ?: continue
                val entry = contactMap[id] ?: continue
                @Suppress("UNCHECKED_CAST")
                val emails = entry.getOrPut("emails") { mutableListOf<String>() } as MutableList<String>
                emails.add(email.trim())
            }
        }

        // 4. Fetch organization info
        val orgCursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE
            ),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )
        orgCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val companyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
            val titleIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val company = cursor.getString(companyIdx) ?: ""
                val title = cursor.getString(titleIdx) ?: ""
                val entry = contactMap[id] ?: continue
                if (company.isNotBlank()) entry["company"] = company
                if (title.isNotBlank()) entry["jobTitle"] = title
            }
        }

        // 5. Build Contact objects
        for ((_, data) in contactMap) {
            val displayName = data["displayName"] as? String ?: ""
            if (displayName.isBlank()) continue

            val nameParts = displayName.trim().split(" ", limit = 2)
            val firstName = nameParts.getOrElse(0) { "" }
            val lastName = nameParts.getOrElse(1) { "" }

            @Suppress("UNCHECKED_CAST")
            val phones = data["phones"] as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val emails = data["emails"] as? List<String> ?: emptyList()
            val company = data["company"] as? String ?: ""
            val jobTitle = data["jobTitle"] as? String ?: ""

            result.add(
                Contact(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumbers = gson.toJson(phones),
                    emails = gson.toJson(emails),
                    company = company,
                    jobTitle = jobTitle,
                    importSource = ImportSource.IOS_CONTACTS.name,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return result
    }
}
