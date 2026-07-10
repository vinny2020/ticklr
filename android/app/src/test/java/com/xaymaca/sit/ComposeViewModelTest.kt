package com.xaymaca.sit

import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.dao.MessageTemplateDao
import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.ContactWithGroups
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.PendingTickleCompletionStore
import com.xaymaca.sit.ui.compose.ComposeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TIC-82: the mark-done prompt is stashed at SMS handoff only when the compose
 * carried a still-valid reminder id for the recipient it was sent to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComposeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ---- Stub DAOs (only the reminder store carries meaningful state) --------

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

    private class StubContactGroupDao : ContactGroupDao {
        override fun getAll(): Flow<List<ContactGroup>> = flowOf(emptyList())
        override suspend fun getById(id: Long): ContactGroup? = null
        override suspend fun getByCategoryId(categoryId: String): ContactGroup? = null
        override suspend fun findByNameCaseInsensitive(name: String): ContactGroup? = null
        override suspend fun countByNameCaseInsensitive(name: String, excludeId: Long): Int = 0
        override suspend fun getGroupWithContacts(id: Long): GroupWithContacts? = null
        override fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> = flowOf(emptyList())
        override suspend fun insert(group: ContactGroup): Long = 0L
        override suspend fun update(group: ContactGroup) {}
        override suspend fun delete(group: ContactGroup) {}
        override suspend fun deleteAll() {}
        override suspend fun deleteAllCrossRefs() {}
        override suspend fun deleteCrossRefsForContact(contactId: Long) {}
        override suspend fun deleteCrossRefsForGroup(groupId: Long) {}
        override suspend fun insertCrossRef(crossRef: ContactGroupCrossRef) {}
        override suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef) {}
        override fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> = flowOf(emptyList())
        override fun getMemberCount(groupId: Long): Flow<Int> = flowOf(0)
        override fun getAllCrossRefs(): Flow<List<ContactGroupCrossRef>> = flowOf(emptyList())
    }

    private class StubMessageTemplateDao : MessageTemplateDao {
        override fun getAll(): Flow<List<MessageTemplate>> = flowOf(emptyList())
        override suspend fun getById(id: Long): MessageTemplate? = null
        override suspend fun count(): Int = 0
        override suspend fun insert(template: MessageTemplate): Long = 0L
        override suspend fun update(template: MessageTemplate) {}
        override suspend fun delete(template: MessageTemplate) {}
    }

    private class FakeTickleReminderDao : TickleReminderDao {
        val reminders = mutableListOf<TickleReminder>()
        override fun getAll(): Flow<List<TickleReminder>> = flowOf(reminders.toList())
        override suspend fun getById(id: Long): TickleReminder? = reminders.find { it.id == id }
        override suspend fun insert(reminder: TickleReminder): Long = reminder.id
        override suspend fun update(reminder: TickleReminder) {}
        override suspend fun delete(reminder: TickleReminder) { reminders.remove(reminder) }
        override fun getByStatus(status: String): Flow<List<TickleReminder>> = flowOf(emptyList())
        override suspend fun getDueReminders(now: Long): List<TickleReminder> = emptyList()
        override suspend fun getArmableReminders(now: Long): List<TickleReminder> = emptyList()
        override suspend fun getByContactId(contactId: Long): List<TickleReminder> = reminders.filter { it.contactId == contactId }
        override suspend fun getByGroupId(groupId: Long): List<TickleReminder> = emptyList()
        override suspend fun deleteByContactId(contactId: Long) {}
        override suspend fun deleteByGroupId(groupId: Long) {}
        override suspend fun deleteAll() {}
    }

    private lateinit var tickleDao: FakeTickleReminderDao
    private lateinit var store: PendingTickleCompletionStore
    private lateinit var viewModel: ComposeViewModel

    private val alice = Contact(id = 1L, firstName = "Alice", lastName = "Smith")
    private val bob = Contact(id = 2L, firstName = "Bob", lastName = "Jones")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tickleDao = FakeTickleReminderDao()
        val contactGroupDao = StubContactGroupDao()
        store = PendingTickleCompletionStore()
        viewModel = ComposeViewModel(
            contactRepository = ContactRepository(StubContactDao(), contactGroupDao, tickleDao),
            messageTemplateRepository = MessageTemplateRepository(StubMessageTemplateDao()),
            contactGroupDao = contactGroupDao,
            tickleRepository = TickleRepository(tickleDao),
            pendingTickleCompletionStore = store,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handoff with an attached active reminder stashes the mark-done prompt`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.ACTIVE.name)
        )
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        val pending = store.pending.value
        assertEquals(10L, pending?.reminderId)
        assertEquals("Alice Smith", pending?.contactName)
    }

    @Test
    fun `handoff without an attached reminder stashes nothing`() = runTest {
        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
    }

    @Test
    fun `handoff skips the prompt when the reminder was already completed`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.COMPLETED.name)
        )
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
    }

    @Test
    fun `handoff skips the prompt when the reminder was deleted`() = runTest {
        // Reminder 10 is attached but absent from the DB (deleted in the meantime).
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
    }

    @Test
    fun `handoff skips the prompt when the recipient was re-targeted`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.ACTIVE.name)
        )
        // Attached for Alice's reminder, but the message is actually sent to Bob.
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(bob)
        advanceUntilIdle()

        assertNull(store.pending.value)
    }

    @Test
    fun `attachReminder treats -1 sentinel as no reminder`() = runTest {
        viewModel.attachReminder(reminderId = -1L, contactId = -1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
    }
}
