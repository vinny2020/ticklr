package com.xaymaca.sit.service

import com.google.gson.Gson
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ImportSource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
/**
 * Parses LinkedIn Connections CSV export.
 * Expected columns: First Name, Last Name, Email Address, Company, Position
 * LinkedIn CSV export path: linkedin.com → Me → Settings → Data privacy → Get a copy of your data
 */
@Singleton
class LinkedInCSVParser @Inject constructor() {

    private val gson = Gson()

    fun parse(inputStream: InputStream): List<Contact> {
        val result = mutableListOf<Contact>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        // Skip any header lines before the column row
        var headerLine: String? = null
        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            // LinkedIn CSVs sometimes have preamble lines; find the real header
            if (trimmed.contains("First Name", ignoreCase = true) &&
                trimmed.contains("Last Name", ignoreCase = true)
            ) {
                headerLine = trimmed
                break
            }
            line = reader.readLine()
        }

        if (headerLine == null) return emptyList()

        val headers = parseCsvRow(headerLine)
        val firstNameIdx = headers.indexOfFirst { it.trim().equals("First Name", ignoreCase = true) }
        val lastNameIdx = headers.indexOfFirst { it.trim().equals("Last Name", ignoreCase = true) }
        val emailIdx = headers.indexOfFirst { it.trim().equals("Email Address", ignoreCase = true) }
        val companyIdx = headers.indexOfFirst { it.trim().equals("Company", ignoreCase = true) }
        val positionIdx = headers.indexOfFirst { it.trim().equals("Position", ignoreCase = true) }
        val phoneIdx = headers.indexOfFirst { it.trim().equals("Phone Number", ignoreCase = true) }

        if (firstNameIdx == -1 && lastNameIdx == -1) return emptyList()

        var dataLine = reader.readLine()
        while (dataLine != null) {
            val trimmed = dataLine.trim()
            if (trimmed.isBlank()) {
                dataLine = reader.readLine()
                continue
            }
            val cols = parseCsvRow(trimmed)

            fun col(idx: Int): String = if (idx >= 0 && idx < cols.size) cols[idx].trim() else ""

            val firstName = col(firstNameIdx)
            val lastName = col(lastNameIdx)
            if (firstName.isBlank() && lastName.isBlank()) {
                dataLine = reader.readLine()
                continue
            }

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
            dataLine = reader.readLine()
        }

        return result
    }

    /**
     * Parses a single CSV row, respecting quoted fields that may contain commas.
     */
    private fun parseCsvRow(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    // Escaped quote inside quoted field
                    current.append('"')
                    i += 2
                }
                ch == '"' -> {
                    inQuotes = !inQuotes
                    i++
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(ch)
                    i++
                }
            }
        }
        fields.add(current.toString())
        return fields
    }
}
