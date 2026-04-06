package com.xaymaca.sit

import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.ContactWithGroups
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.data.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
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
    }

    // Minimal stub — ContactGroupDao is not under test here
    private class StubContactGroupDao : ContactGroupDao {
        override fun getAll(): Flow<List<ContactGroup>> = flowOf(emptyList())
        override suspend fun getById(id: Long): ContactGroup? = null
        override suspend fun getGroupWithContacts(id: Long): GroupWithContacts? = null
        override fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> = flowOf(emptyList())
        override suspend fun insert(group: ContactGroup): Long = 0L
        override suspend fun update(group: ContactGroup) {}
        override suspend fun delete(group: ContactGroup) {}
        override suspend fun deleteAll() {}
        override suspend fun deleteAllCrossRefs() {}
        override suspend fun insertCrossRef(crossRef: ContactGroupCrossRef) {}
        override suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef) {}
        override fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> = flowOf(emptyList())
        override fun getMemberCount(groupId: Long): Flow<Int> = flowOf(0)
    }

    private lateinit var contactDao: FakeContactDao
    private lateinit var repository: ContactRepository

    private fun makeContact(id: Long, first: String, last: String) = Contact(
        id = id, firstName = first, lastName = last
    )

    @Before
    fun setUp() {
        contactDao = FakeContactDao()
        repository = ContactRepository(contactDao, StubContactGroupDao())
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
}
