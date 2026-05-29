package com.xaymaca.sit

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that every string key in the base English strings.xml has a corresponding
 * entry in each translated strings.xml. Missing keys silently fall back to English,
 * which creates a jarring mixed-language UI — this test catches the gap at compile time.
 */
class TranslationCompletenessTest {

    private val baseFile = File("src/main/res/values/strings.xml")

    private val locales = mapOf(
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "nl" to "Dutch",
        "el" to "Greek",
        "pl" to "Polish",
        "ro" to "Romanian",
        "hu" to "Hungarian",
        "pt" to "Portuguese",
        "sv" to "Swedish",
        "cs" to "Czech",
        "ru" to "Russian",
        "zh" to "Chinese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "hi" to "Hindi",
        "ar" to "Arabic",
        "he" to "Hebrew",
        "ur" to "Urdu",
    )

    @Test
    fun `all translation files have all keys from base strings xml`() {
        val baseKeys = extractStringKeys(baseFile)
        val failures = mutableListOf<String>()

        locales.forEach { (code, name) ->
            val file = File("src/main/res/values-$code/strings.xml")
            assertTrue(file.exists(), "$name strings.xml should exist at ${file.absolutePath}")
            val missing = baseKeys - extractStringKeys(file)
            if (missing.isNotEmpty()) {
                failures += "$name (values-$code) missing ${missing.size} key(s): ${missing.sorted().joinToString()}"
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `all translation files have no extra keys not in base`() {
        val baseKeys = extractStringKeys(baseFile)
        val failures = mutableListOf<String>()

        locales.forEach { (code, name) ->
            val file = File("src/main/res/values-$code/strings.xml")
            if (!file.exists()) return@forEach  // covered by the other test
            val extra = extractStringKeys(file) - baseKeys
            if (extra.isNotEmpty()) {
                failures += "$name (values-$code) has ${extra.size} extra key(s) not in base: ${extra.sorted().joinToString()}"
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `hebrew iw alias mirrors values-he`() {
        // java.util.Locale normalizes "he" -> "iw", so Android resolves Hebrew under
        // the legacy iw code and won't match the he-tagged resources. values-iw must
        // exist and mirror values-he so Hebrew renders. Comments are ignored so the
        // alias may carry an explanatory header.
        val heFile = File("src/main/res/values-he/strings.xml")
        val iwFile = File("src/main/res/values-iw/strings.xml")
        assertTrue(
            iwFile.exists(),
            "values-iw/strings.xml must exist (legacy Hebrew alias). Copy values-he/strings.xml to values-iw/strings.xml.",
        )
        assertEquals(
            meaningfulLines(heFile),
            meaningfulLines(iwFile),
            "values-iw must mirror values-he (ignoring comments). Re-copy values-he/strings.xml to values-iw/strings.xml after editing Hebrew.",
        )
    }

    // File content minus the XML prologue, comments, and blank lines — used to
    // compare two strings.xml files for identical string/plurals content.
    private fun meaningfulLines(file: File): List<String> =
        file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("<!--") && !it.startsWith("<?xml") }

    // Extracts all <string name="..."> and <plurals name="..."> keys from a strings.xml file.
    private fun extractStringKeys(file: File): Set<String> {
        val stringRegex = Regex("""<string name="([^"]+)">""")
        val pluralsRegex = Regex("""<plurals name="([^"]+)">""")
        return file.readLines().flatMap { line ->
            stringRegex.findAll(line).map { it.groupValues[1] } +
                pluralsRegex.findAll(line).map { it.groupValues[1] }
        }.toSet()
    }
}
