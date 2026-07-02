package com.xaymaca.sit.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only resolver for system Contacts photos. Matches a Ticklr
 * `Contact` against `ContactsContract` entries by normalised phone
 * number or lowercase email — never by name. Returns null if no match
 * or no photo. Caches results for the session.
 *
 * Authorization piggybacks on the existing READ_CONTACTS permission
 * (granted during the import flow). Silently returns null if the
 * permission has not been granted.
 */
object ContactPhotoService {

    private val cache = LruCache<Long, Bitmap>(64)

    fun cached(contactId: Long): Bitmap? = cache.get(contactId)

    fun clearCache() {
        cache.evictAll()
    }

    /**
     * Evict a single contact's cached photo. Must be called on delete:
     * because the cache is keyed by contact id and SQLite reuses the max
     * rowid after the newest row is removed, a new contact can inherit a
     * deleted contact's id — and without eviction it would show the deleted
     * contact's stale cached photo. (TIC-72)
     */
    fun evict(contactId: Long) {
        cache.remove(contactId)
    }

    suspend fun fetch(
        context: Context,
        contactId: Long,
        phoneNumbers: List<String>,
        emails: List<String>,
    ): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(contactId)?.let { return@withContext it }

        // Silently bail when READ_CONTACTS isn't granted — mirrors iOS
        // ContactPhotoFetcher's CNContactStore.authorizationStatus check.
        // Without this, calling ContentResolver.query against
        // ContactsContract crashes with SecurityException on Samsung
        // and any other device where the user hasn't (or hasn't yet)
        // granted contacts access.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        val normalisedPhones = phoneNumbers.map { normalisePhone(it) }.filter { it.isNotEmpty() }
        val normalisedEmails = emails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (normalisedPhones.isEmpty() && normalisedEmails.isEmpty()) return@withContext null

        // Try phone-number lookup first via PhoneLookup (fast path).
        for (phone in normalisedPhones) {
            val rawId = lookupRawContactIdByPhone(context, phone)
            if (rawId != null) {
                val bmp = decodePhotoForContact(context, rawId)
                if (bmp != null) {
                    cache.put(contactId, bmp)
                    return@withContext bmp
                }
            }
        }

        // Fall back to email lookup.
        for (email in normalisedEmails) {
            val rawId = lookupRawContactIdByEmail(context, email)
            if (rawId != null) {
                val bmp = decodePhotoForContact(context, rawId)
                if (bmp != null) {
                    cache.put(contactId, bmp)
                    return@withContext bmp
                }
            }
        }

        return@withContext null
    }

    private fun lookupRawContactIdByPhone(context: Context, phone: String): Long? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return null
    }

    private fun lookupRawContactIdByEmail(context: Context, email: String): Long? {
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI,
            Uri.encode(email),
        )
        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return null
    }

    private fun decodePhotoForContact(context: Context, systemContactId: Long): Bitmap? {
        val contactUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(systemContactId.toString())
            .build()
        // Try high-res first, fall back to thumbnail.
        val displayPhoto = Uri.withAppendedPath(
            contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO,
        )
        return try {
            context.contentResolver.openAssetFileDescriptor(displayPhoto, "r")?.use { afd ->
                BitmapFactory.decodeStream(afd.createInputStream())
            } ?: decodeThumbnail(context, systemContactId)
        } catch (_: Exception) {
            decodeThumbnail(context, systemContactId)
        }
    }

    private fun decodeThumbnail(context: Context, systemContactId: Long): Bitmap? {
        val contactUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(systemContactId.toString())
            .build()
        val thumbStream = ContactsContract.Contacts.openContactPhotoInputStream(
            context.contentResolver, contactUri, /* preferHighres = */ false,
        )
        return thumbStream?.use { BitmapFactory.decodeStream(it) }
    }

    /** Digits-only, last 10 — mirrors iOS phone matching strategy. */
    private fun normalisePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return digits.takeLast(10)
    }
}
