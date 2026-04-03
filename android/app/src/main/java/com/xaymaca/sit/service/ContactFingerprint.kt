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
 * A blank fingerprint ("") is treated as unset — contacts without a name
 * AND without any phone/email are not fingerprinted and can always be inserted.
 */
object ContactFingerprint {

    fun compute(
        firstName: String,
        lastName: String,
        phoneNumbersJson: String,
        emailsJson: String
    ): String {
        val first = firstName.trim().lowercase()
        val last = lastName.trim().lowercase()

        val phones: List<String> = parseJsonStringArray(phoneNumbersJson)
        val emails: List<String> = parseJsonStringArray(emailsJson)

        val primaryPhone = phones.firstOrNull()?.replace(Regex("[^0-9]"), "") ?: ""
        val primaryEmail = emails.firstOrNull()?.trim()?.lowercase() ?: ""
        val contactKey = primaryPhone.ifBlank { primaryEmail }

        // Don't fingerprint contacts with no identifying info
        if (first.isBlank() && last.isBlank() && contactKey.isBlank()) return ""

        val raw = "$first|$last|$contactKey"
        return sha1(raw)
    }

    private fun parseJsonStringArray(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
        return trimmed
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
