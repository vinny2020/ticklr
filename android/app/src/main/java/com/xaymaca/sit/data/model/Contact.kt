package com.xaymaca.sit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    /** JSON array of phone number strings */
    val phoneNumbers: String = "[]",
    /** JSON array of email strings */
    val emails: String = "[]",
    val company: String = "",
    val jobTitle: String = "",
    val notes: String = "",
    /** JSON array of tag strings */
    val tags: String = "[]",
    val importSource: String = ImportSource.MANUAL.name,
    val createdAt: Long = System.currentTimeMillis(),
    val lastContactedAt: Long = 0L,
    /** SHA-1 fingerprint for deduplication. Empty string = unset (not deduplicated). */
    val fingerprint: String = ""
) {
    val fullName: String get() = "$firstName $lastName".trim()

    val initials: String
        get() {
            val f = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            val l = lastName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            return (f + l).ifEmpty { "?" }
        }
}
