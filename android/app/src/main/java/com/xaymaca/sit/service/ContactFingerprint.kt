package com.xaymaca.sit.service

import java.security.MessageDigest

/**
 * Generates a stable deduplication fingerprint for a contact.
 *
 * Strategy (mirrors iOS implementation):
 *   fingerprint = SHA-1( normalize(firstName) + "|" + normalize(lastName) + "|" + normalize(primaryPhone or primaryEmail) )
 *
 * Normalization:
 *   - Trim and lowercase
 *   - Phone: strip all non-digit characters
 *   - Email: trim and lowercase
 *
 * A blank fingerprint ("") is treated as unset — a contact is only
 * deduplicated when it has a phone number OR an email. A name alone is
 * not a reliable identity (LinkedIn omits email for most connections, so
 * two different "John Smith"s must not collide) so name-only contacts get
 * a blank fingerprint and are always inserted. iOS mirrors this rule.
 */
object ContactFingerprint {

    // Single source of truth for JSON-array parsing — the same converter Room
    // uses for the phoneNumbers/emails columns, so fingerprinting sees exactly
    // the values that were stored (dial pauses, commas and quotes intact).
    private val listConverter = StringListConverter()

    fun compute(
        firstName: String,
        lastName: String,
        phoneNumbersJson: String,
        emailsJson: String
    ): String {
        val first = firstName.trim().lowercase()
        val last = lastName.trim().lowercase()

        val phones: List<String> = listConverter.fromString(phoneNumbersJson)
        val emails: List<String> = listConverter.fromString(emailsJson)

        val primaryPhone = phones.firstOrNull()?.replace(Regex("[^0-9]"), "") ?: ""
        val primaryEmail = emails.firstOrNull()?.trim()?.lowercase() ?: ""
        val contactKey = primaryPhone.ifBlank { primaryEmail }

        // No phone AND no email → not enough to safely dedup on. Leave unset.
        if (contactKey.isBlank()) return ""

        val raw = "$first|$last|$contactKey"
        return sha1(raw)
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
