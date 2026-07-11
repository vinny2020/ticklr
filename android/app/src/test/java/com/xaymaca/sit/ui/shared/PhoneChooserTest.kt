package com.xaymaca.sit.ui.shared

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TIC-96: every Send/Call entry point (ComposeScreen, ContactDetailScreen,
 * TickleActionSheet, Network row long-press) routes its phone list through this
 * single decision instead of silently reaching for `phones.firstOrNull()`.
 */
class PhoneChooserTest {

    @Test
    fun `no phones resolves to None`() {
        assertEquals(PhoneChoice.None, PhoneChooser.choose(emptyList()))
    }

    @Test
    fun `a single phone takes the direct fast path`() {
        val choice = PhoneChooser.choose(listOf("555-1234"))
        assertEquals(PhoneChoice.Direct("555-1234"), choice)
    }

    @Test
    fun `more than one phone requires an explicit choice`() {
        val choice = PhoneChooser.choose(listOf("555-1234", "555-5678"))
        assertTrue(choice is PhoneChoice.NeedsChoice)
        assertEquals(listOf("555-1234", "555-5678"), (choice as PhoneChoice.NeedsChoice).numbers)
    }

    @Test
    fun `needs-choice preserves the original order`() {
        val phones = listOf("c", "a", "b")
        val choice = PhoneChooser.choose(phones) as PhoneChoice.NeedsChoice
        assertEquals(phones, choice.numbers)
    }
}
