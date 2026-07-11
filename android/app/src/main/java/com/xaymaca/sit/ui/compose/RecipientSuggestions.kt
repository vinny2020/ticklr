package com.xaymaca.sit.ui.compose

import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.service.TickleScheduler

/**
 * The recipient-picker suggestions shown when the "To" field is focused with an
 * empty query (TIC-86). Three ordered, non-overlapping buckets:
 *
 * - [dueToday]: contacts with a currently-due reminder ([TickleScheduler.isDue]),
 *   most-overdue first, so "who do I owe a message right now" is at the top.
 * - [recents]: contacts reached out to most recently ([Contact.lastContactedAt]
 *   descending), excluding anyone already in [dueToday], capped at [RECENTS_CAP].
 * - [all]: every contact, alphabetical — the browse-all list revealed behind the
 *   "All contacts…" affordance so the user can pick without typing.
 *
 * Non-empty queries bypass this entirely and use ComposeViewModel's filtered
 * `contacts` flow (unchanged recall behavior).
 */
data class RecipientSuggestions(
    val dueToday: List<Contact>,
    val recents: List<Contact>,
    val all: List<Contact>,
) {
    val isEmpty: Boolean get() = all.isEmpty()

    companion object {
        val EMPTY = RecipientSuggestions(emptyList(), emptyList(), emptyList())
    }
}

/**
 * Pure assembly of [RecipientSuggestions] from the raw contact + reminder lists.
 * Kept side-effect-free (time is passed in) so the ordering/exclusion/cap rules
 * are unit-testable without a ViewModel or the clock.
 */
object RecipientSuggestionAssembler {

    /** Recents section cap (§2 of TIC-86). */
    const val RECENTS_CAP = 5

    fun assemble(
        contacts: List<Contact>,
        reminders: List<TickleReminder>,
        now: Long,
    ): RecipientSuggestions {
        if (contacts.isEmpty()) return RecipientSuggestions.EMPTY

        val contactsById = contacts.associateBy { it.id }

        // For each contact with due reminder(s), the most-overdue nextDueDate
        // (smallest = furthest in the past). Contacts whose reminder points at a
        // now-deleted id are dropped by the contactsById guard.
        val mostOverdueByContact: Map<Long, Long> = reminders
            .asSequence()
            .filter { TickleScheduler.isDue(it, now) }
            .mapNotNull { r -> r.contactId?.let { id -> id to r.nextDueDate } }
            .filter { contactsById.containsKey(it.first) }
            .groupingBy { it.first }
            .fold(Long.MAX_VALUE) { min, (_, dueDate) -> minOf(min, dueDate) }

        val dueToday: List<Contact> = mostOverdueByContact.entries
            .sortedBy { it.value } // ascending nextDueDate == most overdue first
            .mapNotNull { contactsById[it.key] }

        val dueIds = mostOverdueByContact.keys

        val recents: List<Contact> = contacts
            .asSequence()
            .filter { it.lastContactedAt > 0L && it.id !in dueIds }
            .sortedByDescending { it.lastContactedAt }
            .take(RECENTS_CAP)
            .toList()

        val all: List<Contact> = contacts.sortedWith(
            compareBy(
                // Unnamed contacts sink to the bottom of the browse list.
                { it.fullName.isBlank() },
                { it.fullName.lowercase() },
            )
        )

        return RecipientSuggestions(dueToday = dueToday, recents = recents, all = all)
    }
}
