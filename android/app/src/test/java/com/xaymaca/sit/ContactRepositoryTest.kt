package com.xaymaca.sit

import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.ContactWithGroups
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactRepositoryTest {

    // Fake ContactDao that tracks calls and holds in-memory state
    private class FakeContactDao : ContactDao {
        val contacts = mutableListOf<Contact>()
        var deleteAllCallCount = 0

        override fun getAll(): Flow<List<Contact>> = flowOf(contacts.toList())
        override suspend fun getById(id: Long): Contact? = contacts.find { it.id == id }
        override suspend fun getContactWithGroups(id: Long): ContactWithGroups? = null
        override suspend fun insert(contact: Contact): Long {
            contacts.add(contact)
            return contact.id
        }
        override suspend fun countByFingerprint(fingerprint: String): Int = contacts.count { it.fingerprint == fingerprint }
        override suspend fun update(contact: Contact) {
            val idx = contacts.indexOfFirst { it.id == contact.id }
            if (idx >= 0) contacts[idx] = contact
        }
        override suspend fun delete(contact: Contact) { contacts.remove(contact) }
        override suspend fun deleteAll() {
            deleteAllCallCount++
            contacts.clear()
        }
        override fun search(query: String): Flow<List<Contact>> =
            flowOf(contacts.filter { it.fullName.contains(query, ignoreCase = true) })
        override fun getContactsForGroup(groupId: Long): Flow<List<Contact>> = flowOf(emptyList())
        override fun getContactsInCategory(categoryId: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun searchContactsInCategory(categoryId: String, query: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun countContactsInCategory(categoryId: String): Flow<Int> = flowOf(0)
    }

    // Fake ContactGroupDao that tracks groups and cross-ref membership rows
    private class FakeContactGroupDao : ContactGroupDao {
        val groups = mutableListOf<ContactGroup>()
        val crossRefs = mutableListOf<ContactGroupCrossRef>()

        override fun getAll(): Flow<List<ContactGroup>> = flowOf(groups.toList())
        override suspend fun getById(id: Long): ContactGroup? = groups.find { it.id == id }
        override suspend fun getByCategoryId(categoryId: String): ContactGroup? = null
        override suspend fun findByNameCaseInsensitive(name: String): ContactGroup? = null
        override suspend fun countByNameCaseInsensitive(name: String, excludeId: Long): Int = 0
        override suspend fun getGroupWithContacts(id: Long): GroupWithContacts? = null
        override fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> = flowOf(emptyList())
        override suspend fun insert(group: ContactGroup): Long {
            groups.add(group)
            return group.id
        }
        override suspend fun update(group: ContactGroup) {}
        override suspend fun delete(group: ContactGroup) { groups.removeAll { it.id == group.id } }
        override suspend fun deleteAll() { groups.clear() }
        override suspend fun deleteAllCrossRefs() { crossRefs.clear() }
        override suspend fun deleteCrossRefsForContact(contactId: Long) {
            crossRefs.removeAll { it.contactId == contactId }
        }
        override suspend fun deleteCrossRefsForGroup(groupId: Long) {
            crossRefs.removeAll { it.groupId == groupId }
        }
        override suspend fun insertCrossRef(crossRef: ContactGroupCrossRef) { crossRefs.add(crossRef) }
        override suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef) { crossRefs.remove(crossRef) }
        override fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> = flowOf(emptyList())
        override fun getMemberCount(groupId: Long): Flow<Int> = flowOf(0)
        override fun getAllCrossRefs(): Flow<List<ContactGroupCrossRef>> = flowOf(crossRefs.toList())
    }

    // Fake TickleReminderDao that holds reminders in memory
    private class FakeTickleReminderDao : TickleReminderDao {
        val reminders = mutableListOf<TickleReminder>()

        override fun getAll(): Flow<List<TickleReminder>> = flowOf(reminders.toList())
        override suspend fun getById(id: Long): TickleReminder? = reminders.find { it.id == id }
        override suspend fun insert(reminder: TickleReminder): Long {
            reminders.add(reminder)
            return reminder.id
        }
        override suspend fun update(reminder: TickleReminder) {}
        override suspend fun delete(reminder: TickleReminder) { reminders.remove(reminder) }
        override fun getByStatus(status: String): Flow<List<TickleReminder>> =
            flowOf(reminders.filter { it.status == status })
        override suspend fun getDueReminders(now: Long): List<TickleReminder> =
            reminders.filter { it.nextDueDate <= now }
        override suspend fun getByContactId(contactId: Long): List<TickleReminder> =
            reminders.filter { it.contactId == contactId }
        override suspend fun getByGroupId(groupId: Long): List<TickleReminder> =
            reminders.filter { it.groupId == groupId }
        override suspend fun deleteByContactId(contactId: Long) {
            reminders.removeAll { it.contactId == contactId }
        }
        override suspend fun deleteByGroupId(groupId: Long) {
            reminders.removeAll { it.groupId == groupId }
        }
        override suspend fun deleteAll() { reminders.clear() }
    }

    private lateinit var contactDao: FakeContactDao
    private lateinit var groupDao: FakeContactGroupDao
    private lateinit var tickleDao: FakeTickleReminderDao
    private lateinit var repository: ContactRepository

    private fun makeContact(id: Long, first: String, last: String) = Contact(
        id = id, firstName = first, lastName = last
    )

    @Before
    fun setUp() {
        contactDao = FakeContactDao()
        groupDao = FakeContactGroupDao()
        tickleDao = FakeTickleReminderDao()
        repository = ContactRepository(contactDao, groupDao, tickleDao)
    }

    @Test
    fun `deleteAllContacts delegates to dao deleteAll`() = runBlocking {
        repository.deleteAllContacts()
        assertEquals(1, contactDao.deleteAllCallCount)
    }

    @Test
    fun `deleteAllContacts clears all contacts from the dao`() = runBlocking {
        contactDao.contacts.add(makeContact(1L, "Alice", "Smith"))
        contactDao.contacts.add(makeContact(2L, "Bob", "Jones"))

        repository.deleteAllContacts()

        assertTrue(contactDao.contacts.isEmpty())
    }

    @Test
    fun `deleteAllContacts called multiple times invokes dao each time`() = runBlocking {
        repository.deleteAllContacts()
        repository.deleteAllContacts()
        assertEquals(2, contactDao.deleteAllCallCount)
    }

    @Test
    fun `deleteContact does not affect deleteAll call count`() = runBlocking {
        val contact = makeContact(1L, "Alice", "Smith")
        contactDao.contacts.add(contact)

        repository.deleteContact(contact)

        assertEquals(0, contactDao.deleteAllCallCount)
        assertTrue(contactDao.contacts.isEmpty())
    }

    @Test
    fun `deleteContact cascades to its tickles and cross-refs but spares others`() = runBlocking {
        val alice = makeContact(1L, "Alice", "Smith")
        val bob = makeContact(2L, "Bob", "Jones")
        contactDao.contacts.add(alice)
        contactDao.contacts.add(bob)
        tickleDao.reminders.add(TickleReminder(id = 10L, contactId = 1L))
        tickleDao.reminders.add(TickleReminder(id = 11L, contactId = 1L))
        tickleDao.reminders.add(TickleReminder(id = 12L, contactId = 2L))
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 1L, groupId = 100L))
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 2L, groupId = 100L))

        repository.deleteContact(alice)

        // Alice's reminders and cross-refs gone; Bob's untouched.
        assertTrue(tickleDao.reminders.none { it.contactId == 1L })
        assertTrue(tickleDao.reminders.any { it.contactId == 2L })
        assertTrue(groupDao.crossRefs.none { it.contactId == 1L })
        assertTrue(groupDao.crossRefs.any { it.contactId == 2L })
        assertFalse(contactDao.contacts.contains(alice))
    }

    @Test
    fun `deleteGroup cascades to its tickles but spares other groups`() = runBlocking {
        val group = ContactGroup(id = 100L, name = "Work")
        tickleDao.reminders.add(TickleReminder(id = 20L, groupId = 100L))
        tickleDao.reminders.add(TickleReminder(id = 21L, groupId = 200L))

        repository.deleteGroup(group)

        assertTrue(tickleDao.reminders.none { it.groupId == 100L })
        assertTrue(tickleDao.reminders.any { it.groupId == 200L })
    }

    // TIC-72: deleting a group must wipe that group's membership rows so a
    // new group reusing the deleted group's rowid can't inherit old members.
    @Test
    fun `deleteGroup wipes that group's cross-refs`() = runBlocking {
        val group = ContactGroup(id = 5L, name = "Old", emoji = "👥")
        groupDao.groups.add(group)
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 1L, groupId = 5L))
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 2L, groupId = 5L))

        repository.deleteGroup(group)

        assertTrue(groupDao.groups.none { it.id == 5L })
        assertTrue(groupDao.crossRefs.none { it.groupId == 5L })
    }

    @Test
    fun `deleteGroup leaves other groups' cross-refs intact`() = runBlocking {
        val target = ContactGroup(id = 5L, name = "Old", emoji = "👥")
        val other = ContactGroup(id = 6L, name = "Keep", emoji = "👥")
        groupDao.groups.add(target)
        groupDao.groups.add(other)
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 1L, groupId = 5L))
        groupDao.crossRefs.add(ContactGroupCrossRef(contactId = 1L, groupId = 6L))

        repository.deleteGroup(target)

        assertEquals(1, groupDao.crossRefs.size)
        assertTrue(groupDao.crossRefs.all { it.groupId == 6L })
    }
}
