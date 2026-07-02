package com.xaymaca.sit

import android.content.Context
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
import com.xaymaca.sit.ui.groups.GroupViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class StubContactDao : ContactDao {
        override fun getAll(): Flow<List<Contact>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Contact? = null
        override suspend fun getContactWithGroups(id: Long): ContactWithGroups? = null
        override suspend fun insert(contact: Contact): Long = 0L
        override suspend fun countByFingerprint(fingerprint: String): Int = 0
        override suspend fun update(contact: Contact) {}
        override suspend fun delete(contact: Contact) {}
        override suspend fun deleteAll() {}
        override fun search(query: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun getContactsForGroup(groupId: Long): Flow<List<Contact>> = flowOf(emptyList())
        override fun getContactsInCategory(categoryId: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun searchContactsInCategory(categoryId: String, query: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun countContactsInCategory(categoryId: String): Flow<Int> = flowOf(0)
    }

    /** Fake that tracks inserted groups in-memory, so name-collision checks are exercised
     *  without going through a real Room database (mirrors [FakeContactDao]-style tests). */
    private class FakeContactGroupDao : ContactGroupDao {
        val groups = mutableListOf<ContactGroup>()

        override fun getAll(): Flow<List<ContactGroup>> = flowOf(groups.toList())
        override suspend fun getById(id: Long): ContactGroup? = groups.find { it.id == id }
        override suspend fun getByCategoryId(categoryId: String): ContactGroup? =
            groups.find { it.categoryId == categoryId }
        override suspend fun findByNameCaseInsensitive(name: String): ContactGroup? =
            groups.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
        override suspend fun countByNameCaseInsensitive(name: String, excludeId: Long): Int =
            groups.count { it.name.trim().equals(name.trim(), ignoreCase = true) && it.id != excludeId }
        override suspend fun getGroupWithContacts(id: Long): GroupWithContacts? = null
        override fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> = flowOf(emptyList())
        override suspend fun insert(group: ContactGroup): Long {
            groups.add(group)
            return group.id
        }
        override suspend fun update(group: ContactGroup) {
            val idx = groups.indexOfFirst { it.id == group.id }
            if (idx >= 0) groups[idx] = group
        }
        override suspend fun delete(group: ContactGroup) { groups.remove(group) }
        override suspend fun deleteAll() { groups.clear() }
        override suspend fun deleteAllCrossRefs() {}
        override suspend fun deleteCrossRefsForContact(contactId: Long) {}
        override suspend fun insertCrossRef(crossRef: ContactGroupCrossRef) {}
        override suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef) {}
        override fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> = flowOf(emptyList())
        override fun getMemberCount(groupId: Long): Flow<Int> = flowOf(0)
        override fun getAllCrossRefs(): Flow<List<ContactGroupCrossRef>> = flowOf(emptyList())
    }

    private lateinit var contactGroupDao: FakeContactGroupDao

    private class StubTickleReminderDao : TickleReminderDao {
        override fun getAll(): Flow<List<TickleReminder>> = flowOf(emptyList())
        override suspend fun getById(id: Long): TickleReminder? = null
        override suspend fun insert(reminder: TickleReminder): Long = 0L
        override suspend fun update(reminder: TickleReminder) {}
        override suspend fun delete(reminder: TickleReminder) {}
        override fun getByStatus(status: String): Flow<List<TickleReminder>> = flowOf(emptyList())
        override suspend fun getDueReminders(now: Long): List<TickleReminder> = emptyList()
        override suspend fun getByContactId(contactId: Long): List<TickleReminder> = emptyList()
        override suspend fun getByGroupId(groupId: Long): List<TickleReminder> = emptyList()
        override suspend fun deleteByContactId(contactId: Long) {}
        override suspend fun deleteByGroupId(groupId: Long) {}
        override suspend fun deleteAll() {}
    }

    private lateinit var viewModel: GroupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        contactGroupDao = FakeContactGroupDao()
        viewModel = GroupViewModel(
            ContactRepository(StubContactDao(), contactGroupDao, StubTickleReminderDao()),
            mockk<Context>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toastMessage is null on init`() {
        assertNull(viewModel.toastMessage.value)
    }

    @Test
    fun `showToast sets message immediately`() = runTest {
        viewModel.showToast("Alice Smith added to Hiking Crew")
        testScheduler.runCurrent()
        assertEquals("Alice Smith added to Hiking Crew", viewModel.toastMessage.value)
    }

    @Test
    fun `showToast clears message after 2 seconds`() = runTest {
        viewModel.showToast("Alice Smith added to Hiking Crew")
        testScheduler.runCurrent()
        advanceTimeBy(2001)
        assertNull(viewModel.toastMessage.value)
    }

    @Test
    fun `showToast message is still present just before 2 seconds`() = runTest {
        viewModel.showToast("Bob Jones added to group")
        testScheduler.runCurrent()
        advanceTimeBy(1999)
        assertEquals("Bob Jones added to group", viewModel.toastMessage.value)
    }

    @Test
    fun `showToast replaces an existing message`() = runTest {
        viewModel.showToast("First message")
        testScheduler.runCurrent()
        viewModel.showToast("Second message")
        testScheduler.runCurrent()
        assertEquals("Second message", viewModel.toastMessage.value)
    }

    @Test
    fun `showToast clears message after 2 seconds even when called twice`() = runTest {
        viewModel.showToast("First message")
        testScheduler.runCurrent()
        advanceTimeBy(500)
        viewModel.showToast("Second message")
        testScheduler.runCurrent()
        advanceTimeBy(2001)
        assertNull(viewModel.toastMessage.value)
    }

    // Regression tests for TIC-71: isGroupNameTaken must detect duplicates via a direct
    // DAO query, without relying on the `groups` StateFlow being collected first — the
    // GroupList/GroupDetail screens that call this never collect `groups`, so a version
    // that reads `groups.value` would always see an empty list here.

    @Test
    fun `isGroupNameTaken is false when no group has that name`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        assertEquals(false, viewModel.isGroupNameTaken("Friends"))
    }

    @Test
    fun `isGroupNameTaken is true for an exact match without collecting the groups flow`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        // Deliberately not reading viewModel.groups anywhere in this test.
        assertEquals(true, viewModel.isGroupNameTaken("Family"))
    }

    @Test
    fun `isGroupNameTaken is case-insensitive`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        assertEquals(true, viewModel.isGroupNameTaken("family"))
        assertEquals(true, viewModel.isGroupNameTaken("FAMILY"))
    }

    @Test
    fun `isGroupNameTaken ignores surrounding whitespace`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        assertEquals(true, viewModel.isGroupNameTaken("  Family  "))
    }

    @Test
    fun `isGroupNameTaken excludes the group being renamed`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        assertEquals(false, viewModel.isGroupNameTaken("Family", excludeId = 1L))
    }

    @Test
    fun `isGroupNameTaken still flags a match against a different group when excluding`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))
        contactGroupDao.groups.add(ContactGroup(id = 2L, name = "Friends", emoji = "👥"))

        assertEquals(true, viewModel.isGroupNameTaken("Friends", excludeId = 1L))
    }

    @Test
    fun `isGroupNameTaken is false for a blank name`() = runTest {
        contactGroupDao.groups.add(ContactGroup(id = 1L, name = "Family", emoji = "👪"))

        assertEquals(false, viewModel.isGroupNameTaken("   "))
    }
}
