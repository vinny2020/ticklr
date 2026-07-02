package com.xaymaca.sit

import com.xaymaca.sit.data.model.ImportSource
import com.xaymaca.sit.service.ContactFingerprint
import com.xaymaca.sit.service.LinkedInCSVParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LinkedInCSVParserTest {

    private lateinit var parser: LinkedInCSVParser
    private val gson = Gson()

    @Before
    fun setUp() {
        parser = LinkedInCSVParser()
    }

    private fun emails(contact: com.xaymaca.sit.data.model.Contact): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(contact.emails, type) ?: emptyList()
    }

    private fun csv(vararg lines: String): java.io.InputStream =
        lines.joinToString("\n").byteInputStream(Charsets.UTF_8)

    @Test
    fun `parses basic row correctly`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "Jane,Doe,jane@example.com,Acme,Engineer"
        )
        val result = parser.parse(input)
        assertEquals(1, result.size)
        val c = result[0]
        assertEquals("Jane", c.firstName)
        assertEquals("Doe", c.lastName)
        assertEquals(listOf("jane@example.com"), emails(c))
        assertEquals("Acme", c.company)
        assertEquals("Engineer", c.jobTitle)
        assertEquals(ImportSource.LINKEDIN.name, c.importSource)
    }

    @Test
    fun `skips rows with blank first and last name`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            ",  ,ghost@example.com,NoName Corp,Ghost"
        )
        val result = parser.parse(input)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles quoted fields containing commas`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "\"Smith, Jr.\",Johnson,sj@example.com,\"Acme, Inc.\",\"VP, Sales\""
        )
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Smith, Jr.", result[0].firstName)
        assertEquals("Acme, Inc.", result[0].company)
        assertEquals("VP, Sales", result[0].jobTitle)
    }

    @Test
    fun `handles escaped double quotes inside quoted fields`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "\"He said \"\"Hello\"\"\",Doe,h@example.com,Corp,Dev"
        )
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("He said \"Hello\"", result[0].firstName)
    }

    @Test
    fun `handles LinkedIn preamble lines before header`() {
        val input = csv(
            "Notes: This is a LinkedIn export.",
            "Exported on 2026-03-15",
            "",
            "First Name,Last Name,Email Address,Company,Position",
            "Alice,Smith,alice@example.com,FAANG,SWE"
        )
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].firstName)
    }

    @Test
    fun `returns empty list when no header row found`() {
        val input = csv(
            "This file has no recognizable headers",
            "Alice,Smith,alice@example.com"
        )
        val result = parser.parse(input)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list for blank input`() {
        val result = parser.parse("".byteInputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parses multiple rows`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "Alice,Smith,alice@example.com,Co A,Dev",
            "Bob,Jones,bob@example.com,Co B,Design",
            "Carol,White,,Co C,PM"
        )
        val result = parser.parse(input)
        assertEquals(3, result.size)
        assertEquals("Alice", result[0].firstName)
        assertEquals("Bob", result[1].firstName)
        assertEquals("Carol", result[2].firstName)
        assertTrue(emails(result[2]).isEmpty())
    }

    @Test
    fun `missing optional columns do not crash`() {
        // Only First Name and Last Name columns present
        val input = csv(
            "First Name,Last Name",
            "Alice,Smith"
        )
        val result = parser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].firstName)
        assertEquals("", result[0].company)
        assertEquals("", result[0].jobTitle)
        assertTrue(emails(result[0]).isEmpty())
    }

    @Test
    fun `skips blank lines between data rows`() {
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "Alice,Smith,alice@example.com,Co A,Dev",
            "",
            "   ",
            "Bob,Jones,bob@example.com,Co B,Design"
        )
        val result = parser.parse(input)
        assertEquals(2, result.size)
    }

    // --- TIC-74: CSV parser edge cases + dedup gaps ---

    @Test
    fun `quoted field with embedded newline stays one record`() {
        // The name and company fields contain a literal newline inside quotes.
        // A line-based parser would split this into a truncated contact plus a
        // garbage row; the character-stream parser must keep it as one record.
        val raw =
            "First Name,Last Name,Email Address,Company,Position\n" +
            "\"Ann\nMarie\",Lee,ann@example.com,\"Acme\nCorp\",Engineer"
        val result = parser.parse(raw.byteInputStream(Charsets.UTF_8))
        assertEquals(1, result.size)
        assertEquals("Ann\nMarie", result[0].firstName)
        assertEquals("Lee", result[0].lastName)
        assertEquals("Acme\nCorp", result[0].company)
        assertEquals(listOf("ann@example.com"), emails(result[0]))
    }

    @Test
    fun `strips UTF-8 BOM on header row so first name is preserved`() {
        // A BOM in front of "First Name" used to make firstNameIdx == -1, which
        // silently blanked every imported contact's first name.
        val raw =
            "\uFEFF" +
            "First Name,Last Name,Email Address,Company,Position\n" +
            "Alice,Smith,alice@example.com,Acme,SWE"
        val result = parser.parse(raw.byteInputStream(Charsets.UTF_8))
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].firstName)
        assertEquals("Smith", result[0].lastName)
    }

    @Test
    fun `duplicate rows within one file share a non-blank fingerprint`() {
        // The parser does not dedup, but two identical connections must produce
        // the SAME non-blank fingerprint so the insert layer collapses them.
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "John,Smith,john@example.com,Acme,Dev",
            "John,Smith,john@example.com,Acme,Dev"
        )
        val result = parser.parse(input)
        assertEquals(2, result.size)
        assertTrue(result[0].fingerprint.isNotBlank())
        assertEquals(result[0].fingerprint, result[1].fingerprint)
    }

    @Test
    fun `name-only contacts get a blank fingerprint so they are never deduped`() {
        // No phone and no email → a name alone is not a reliable identity.
        // Two different-but-same-named people must both import (mirrors iOS).
        val input = csv(
            "First Name,Last Name,Email Address,Company,Position",
            "John,Smith,,,",
            "John,Smith,,,"
        )
        val result = parser.parse(input)
        assertEquals(2, result.size)
        assertTrue(result[0].fingerprint.isBlank())
        assertTrue(result[1].fingerprint.isBlank())
    }

    @Test
    fun `dial-pause phone number is fingerprinted whole, not truncated at a comma`() {
        // A phone stored as JSON "555-1234,,101" (dial pause) must be parsed as a
        // single value. The old comma-splitting parser truncated it to "555-1234",
        // colliding two distinct numbers. Uses StringListConverter now.
        val withPause = ContactFingerprint.compute("John", "Smith", "[\"555-1234,,101\"]", "[]")
        val truncated = ContactFingerprint.compute("John", "Smith", "[\"555-1234\"]", "[]")
        assertTrue(withPause.isNotBlank())
        assertNotEquals(withPause, truncated)
    }
}
