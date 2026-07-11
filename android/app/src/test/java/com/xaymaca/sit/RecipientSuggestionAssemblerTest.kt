package com.xaymaca.sit

import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.ui.compose.RecipientSuggestionAssembler
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TIC-86: the pure ordering / exclusion / cap rules behind the "To" field's
 * browse-mode suggestions. No ViewModel, no clock — `now` is passed in.
 */
class RecipientSuggestionAssemblerTest {

    private val now = 1_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun contact(id: Long, first: String = "C$id", last: String = "", lastContacted: Long = 0L) =
        Contact(id = id, firstName = first, lastName = last, lastContactedAt = lastContacted)

    private fun dueReminder(id: Long, contactId: Long, dueAt: Long, status: String = TickleStatus.ACTIVE.name) =
        TickleReminder(id = id, contactId = contactId, nextDueDate = dueAt, status = status)

    @Test
    fun `empty contacts yields empty suggestions`() {
        val result = RecipientSuggestionAssembler.assemble(emptyList(), emptyList(), now)
        assertTrue(result.dueToday.isEmpty())
        assertTrue(result.recents.isEmpty())
        assertTrue(result.all.isEmpty())
        assertTrue(result.isEmpty)
    }

    @Test
    fun `due section orders most overdue first`() {
        val a = contact(1)
        val b = contact(2)
        val c = contact(3)
        val contacts = listOf(a, b, c)
        val reminders = listOf(
            dueReminder(10, contactId = 2, dueAt = now - 1 * day),  // b: 1 day overdue
            dueReminder(11, contactId = 1, dueAt = now - 5 * day),  // a: 5 days overdue (most)
            dueReminder(12, contactId = 3, dueAt = now - 3 * day),  // c: 3 days overdue
        )

        val result = RecipientSuggestionAssembler.assemble(contacts, reminders, now)

        assertEquals(listOf(1L, 3L, 2L), result.dueToday.map { it.id })
    }

    @Test
    fun `contact with multiple due reminders ranks by its most overdue one`() {
        val a = contact(1)
        val b = contact(2)
        val reminders = listOf(
            dueReminder(10, contactId = 1, dueAt = now - 1 * day),  // a also has a 1-day one
            dueReminder(11, contactId = 1, dueAt = now - 9 * day),  // a's most overdue: 9 days
            dueReminder(12, contactId = 2, dueAt = now - 4 * day),  // b: 4 days
        )

        val result = RecipientSuggestionAssembler.assemble(listOf(a, b), reminders, now)

        // a appears once, ahead of b because its worst overdue (9d) beats b's (4d).
        assertEquals(listOf(1L, 2L), result.dueToday.map { it.id })
    }

    @Test
    fun `future and completed reminders are not due`() {
        val a = contact(1)
        val reminders = listOf(
            dueReminder(10, contactId = 1, dueAt = now + 3 * day),                                   // future
            dueReminder(11, contactId = 1, dueAt = now - 1 * day, status = TickleStatus.COMPLETED.name), // completed
        )

        val result = RecipientSuggestionAssembler.assemble(listOf(a), reminders, now)

        assertTrue(result.dueToday.isEmpty())
    }

    @Test
    fun `reminders for absent contacts are ignored`() {
        val a = contact(1)
        val reminders = listOf(dueReminder(10, contactId = 99, dueAt = now - 2 * day))

        val result = RecipientSuggestionAssembler.assemble(listOf(a), reminders, now)

        assertTrue(result.dueToday.isEmpty())
    }

    @Test
    fun `recents are sorted by lastContactedAt descending and exclude never-contacted`() {
        val a = contact(1, lastContacted = now - 10 * day)
        val b = contact(2, lastContacted = now - 2 * day)
        val c = contact(3, lastContacted = 0L) // never contacted -> excluded

        val result = RecipientSuggestionAssembler.assemble(listOf(a, b, c), emptyList(), now)

        assertEquals(listOf(2L, 1L), result.recents.map { it.id })
    }

    @Test
    fun `recents exclude contacts already in the due section`() {
        val a = contact(1, lastContacted = now - 1 * day) // recently contacted AND due
        val b = contact(2, lastContacted = now - 2 * day)
        val reminders = listOf(dueReminder(10, contactId = 1, dueAt = now - 1 * day))

        val result = RecipientSuggestionAssembler.assemble(listOf(a, b), reminders, now)

        assertEquals(listOf(1L), result.dueToday.map { it.id })
        assertEquals(listOf(2L), result.recents.map { it.id }) // a is not duplicated into recents
    }

    @Test
    fun `recents are capped at five`() {
        val contacts = (1L..8L).map { contact(it, lastContacted = now - it * day) }

        val result = RecipientSuggestionAssembler.assemble(contacts, emptyList(), now)

        assertEquals(RecipientSuggestionAssembler.RECENTS_CAP, result.recents.size)
        // Most-recent five: ids 1..5 (smallest offset = most recent).
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result.recents.map { it.id })
    }

    @Test
    fun `all is alphabetical with unnamed contacts last`() {
        val zoe = contact(1, first = "Zoe", last = "Adams")
        val amy = contact(2, first = "amy", last = "brown") // lowercase -> case-insensitive
        val blank = contact(3, first = "", last = "")

        val result = RecipientSuggestionAssembler.assemble(listOf(zoe, amy, blank), emptyList(), now)

        assertEquals(listOf(2L, 1L, 3L), result.all.map { it.id })
    }
}
