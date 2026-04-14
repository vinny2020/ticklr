package com.xaymaca.sit

import org.junit.Test
import java.io.File
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
