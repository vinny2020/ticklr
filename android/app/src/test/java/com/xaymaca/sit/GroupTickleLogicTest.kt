package com.xaymaca.sit

import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.ui.network.NetworkGroupFilter
import com.xaymaca.sit.ui.tickle.tickleBinding
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TIC-88 pure-logic coverage for the group features:
 *  - [tickleBinding]: group-tickle save semantics (groupId set, contact null).
 *  - [NetworkGroupFilter.filter]: the user-group Network filter predicate.
 *  - [TickleScheduler.reminderDisplayName]: alarm name fallback for group tickles.
 */
class GroupTickleLogicTest {

    // --- Group-tickle save semantics ---

    @Test
    fun `group-bound tickle stores groupId and null contact`() {
        val (contactId, groupId) = tickleBinding(groupBound = true, contactId = 7L, groupId = 5L)
        assertEquals(null, contactId)
        assertEquals(5L, groupId)
    }

    @Test
    fun `group-bound tickle nulls the contact even if one was somehow set`() {
        // Defends the invariant: a group tickle never carries a contactId.
        val (contactId, _) = tickleBinding(groupBound = true, contactId = 99L, groupId = 5L)
        assertEquals(null, contactId)
    }

    @Test
    fun `contact-bound tickle stores contactId and null group`() {
        val (contactId, groupId) = tickleBinding(groupBound = false, contactId = 7L, groupId = 5L)
        assertEquals(7L, contactId)
        assertEquals(null, groupId)
    }

    @Test
    fun `binding is mutually exclusive - never both set`() {
        listOf(true, false).forEach { bound ->
            val (c, g) = tickleBinding(groupBound = bound, contactId = 1L, groupId = 2L)
            assertTrue(c == null || g == null, "contact and group must not both be set")
        }
    }

    // --- User-group Network filter predicate ---

    private fun contact(id: Long, first: String, last: String = "", company: String = "") =
        Contact(id = id, firstName = first, lastName = last, company = company)

    private val members = listOf(
        contact(1L, "Alice", "Adams", company = "Acme"),
        contact(2L, "Bob", "Brown", company = "Globex"),
        contact(3L, "Carol", "Chen", company = "Acme"),
    )

    @Test
    fun `blank query returns all members unchanged`() {
        assertEquals(members, NetworkGroupFilter.filter(members, ""))
    }

    @Test
    fun `whitespace-only query returns all members`() {
        assertEquals(members, NetworkGroupFilter.filter(members, "   "))
    }

    @Test
    fun `query matches on first name case-insensitively`() {
        val result = NetworkGroupFilter.filter(members, "alice")
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `query matches on company and can return several members`() {
        val result = NetworkGroupFilter.filter(members, "Acme")
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `query with no match returns empty`() {
        assertTrue(NetworkGroupFilter.filter(members, "zzz").isEmpty())
    }

    @Test
    fun `filtering an empty member list stays empty`() {
        assertTrue(NetworkGroupFilter.filter(emptyList(), "Acme").isEmpty())
    }

    // --- Alarm name fallback (contact -> group -> generic) ---

    @Test
    fun `display name prefers the contact name`() {
        assertEquals(
            "Alice Adams",
            TickleScheduler.reminderDisplayName("Alice Adams", "Hiking Crew", "Someone"),
        )
    }

    @Test
    fun `display name falls back to group name for a group tickle`() {
        assertEquals(
            "Hiking Crew",
            TickleScheduler.reminderDisplayName(null, "Hiking Crew", "Someone"),
        )
    }

    @Test
    fun `display name falls back to group when contact name is blank`() {
        assertEquals(
            "Hiking Crew",
            TickleScheduler.reminderDisplayName("   ", "Hiking Crew", "Someone"),
        )
    }

    @Test
    fun `display name falls back to generic when neither is present`() {
        assertEquals("Someone", TickleScheduler.reminderDisplayName(null, null, "Someone"))
    }

    @Test
    fun `display name falls back to generic when both are blank`() {
        assertEquals("Someone", TickleScheduler.reminderDisplayName("", "  ", "Someone"))
    }
}
