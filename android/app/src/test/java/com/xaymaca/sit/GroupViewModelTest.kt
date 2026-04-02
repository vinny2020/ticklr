package com.xaymaca.sit

import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.ContactWithGroups
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.ui.groups.GroupViewModel
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
        override suspend fun update(contact: Contact) {}
        override suspend fun delete(contact: Contact) {}
        override suspend fun deleteAll() {}
        override fun search(query: String): Flow<List<Contact>> = flowOf(emptyList())
        override fun getContactsForGroup(groupId: Long): Flow<List<Contact>> = flowOf(emptyList())
    }

    private class StubContactGroupDao : ContactGroupDao {
        override fun getAll(): Flow<List<ContactGroup>> = flowOf(emptyList())
        override suspend fun getById(id: Long): ContactGroup? = null
        override suspend fun getGroupWithContacts(id: Long): GroupWithContacts? = null
        override fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> = flowOf(emptyList())
        override suspend fun insert(group: ContactGroup): Long = 0L
        override suspend fun update(group: ContactGroup) {}
        override suspend fun delete(group: ContactGroup) {}
        override suspend fun insertCrossRef(crossRef: ContactGroupCrossRef) {}
        override suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef) {}
        override fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> = flowOf(emptyList())
        override fun getMemberCount(groupId: Long): Flow<Int> = flowOf(0)
    }

    private lateinit var viewModel: GroupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GroupViewModel(ContactRepository(StubContactDao(), StubContactGroupDao()))
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
}
