package com.xaymaca.sit.service

import com.google.gson.Gson
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ImportSource
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
/**
 * Parses LinkedIn Connections CSV export.
 * Expected columns: First Name, Last Name, Email Address, Company, Position
 * LinkedIn CSV export path: linkedin.com → Me → Settings → Data privacy → Get a copy of your data
 *
 * The whole file is tokenised as a character stream (RFC 4180) rather than
 * line-by-line, so a quoted field may contain embedded commas AND newlines
 * without splitting one connection across two records. A leading UTF-8 BOM is
 * stripped so the first header column ("First Name") still matches.
 * Kept in lockstep with the iOS LinkedInCSVParser.
 */
@Singleton
class LinkedInCSVParser @Inject constructor() {

    private val gson = Gson()

    fun parse(inputStream: InputStream): List<Contact> {
        val text = inputStream.reader(Charsets.UTF_8).use { it.readText() }
        val records = parseCsv(text)

        // LinkedIn exports carry preamble/notes lines before the column row.
        val headerIdx = records.indexOfFirst { isHeaderRow(it) }
        if (headerIdx == -1) return emptyList()

        val headers = records[headerIdx]
        val firstNameIdx = headers.indexOfFirst { it.trim().equals("First Name", ignoreCase = true) }
        val lastNameIdx = headers.indexOfFirst { it.trim().equals("Last Name", ignoreCase = true) }
        val emailIdx = headers.indexOfFirst { it.trim().equals("Email Address", ignoreCase = true) }
        val companyIdx = headers.indexOfFirst { it.trim().equals("Company", ignoreCase = true) }
        val positionIdx = headers.indexOfFirst { it.trim().equals("Position", ignoreCase = true) }
        val phoneIdx = headers.indexOfFirst { it.trim().equals("Phone Number", ignoreCase = true) }

        if (firstNameIdx == -1 && lastNameIdx == -1) return emptyList()

        val result = mutableListOf<Contact>()
        for (cols in records.subList(headerIdx + 1, records.size)) {
            if (cols.all { it.isBlank() }) continue

            fun col(idx: Int): String = if (idx >= 0 && idx < cols.size) cols[idx].trim() else ""

            val firstName = col(firstNameIdx)
            val lastName = col(lastNameIdx)
            if (firstName.isBlank() && lastName.isBlank()) continue

            val email = col(emailIdx)
            val emails: List<String> = if (email.isNotBlank()) listOf(email) else emptyList()

            val phone = col(phoneIdx)
            val phones: List<String> = if (phone.isNotBlank()) listOf(phone) else emptyList()

            val phoneNumbersJson = gson.toJson(phones)
            val emailsJson = gson.toJson(emails)
            val fingerprint = ContactFingerprint.compute(firstName, lastName, phoneNumbersJson, emailsJson)

            result.add(
                Contact(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumbers = phoneNumbersJson,
                    emails = emailsJson,
                    company = col(companyIdx),
                    jobTitle = col(positionIdx),
                    importSource = ImportSource.LINKEDIN.name,
                    createdAt = System.currentTimeMillis(),
                    fingerprint = fingerprint
                )
            )
        }

        return result
    }

    private fun isHeaderRow(fields: List<String>): Boolean =
        fields.any { it.trim().equals("First Name", ignoreCase = true) } &&
            fields.any { it.trim().equals("Last Name", ignoreCase = true) }

    /**
     * Tokenises an entire CSV document into records of fields (RFC 4180).
     * Quoted fields may contain commas, CR/LF, and escaped quotes ("").
     * A leading UTF-8 BOM is stripped. Blank trailing lines are dropped.
     */
    private fun parseCsv(input: String): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        var record = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            record.add(current.toString())
            current.clear()
        }
        fun endRecord() {
            endField()
            records.add(record)
            record = mutableListOf()
        }

        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> when {
                    ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        current.append('"'); i += 2
                    }
                    ch == '"' -> { inQuotes = false; i++ }
                    else -> { current.append(ch); i++ }
                }
                ch == '"' -> { inQuotes = true; i++ }
                ch == ',' -> { endField(); i++ }
                ch == '\r' -> {
                    endRecord()
                    i += if (i + 1 < text.length && text[i + 1] == '\n') 2 else 1
                }
                ch == '\n' -> { endRecord(); i++ }
                else -> { current.append(ch); i++ }
            }
        }
        // Flush the final field/record unless the file ended on a newline.
        if (current.isNotEmpty() || record.isNotEmpty()) {
            endRecord()
        }
        return records
    }
}
