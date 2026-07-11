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
import com.xaymaca.sit.service.PendingTickleOfferStore
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TIC-82/TIC-86: at SMS handoff, `recordHandoff` stashes at most one follow-up —
 * the mark-done prompt when the compose carried a still-valid reminder id for the
 * recipient it was sent to; otherwise the create-a-tickle offer, but only when
 * the recipient has no live (non-COMPLETED) reminder already; otherwise nothing.
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
    private lateinit var offerStore: PendingTickleOfferStore
    private lateinit var viewModel: ComposeViewModel

    private val alice = Contact(id = 1L, firstName = "Alice", lastName = "Smith")
    private val bob = Contact(id = 2L, firstName = "Bob", lastName = "Jones")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tickleDao = FakeTickleReminderDao()
        val contactGroupDao = StubContactGroupDao()
        store = PendingTickleCompletionStore()
        offerStore = PendingTickleOfferStore()
        viewModel = ComposeViewModel(
            contactRepository = ContactRepository(StubContactDao(), contactGroupDao, tickleDao),
            messageTemplateRepository = MessageTemplateRepository(StubMessageTemplateDao()),
            contactGroupDao = contactGroupDao,
            tickleRepository = TickleRepository(tickleDao),
            pendingTickleCompletionStore = store,
            pendingTickleOfferStore = offerStore,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handoff with an attached active reminder stashes the mark-done prompt and no offer`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.ACTIVE.name)
        )
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        val pending = store.pending.value
        assertEquals(10L, pending?.reminderId)
        assertEquals("Alice Smith", pending?.contactName)
        // TIC-86 exclusivity: the mark-done prompt wins, so no offer is stashed.
        assertNull(offerStore.pending.value)
    }

    @Test
    fun `plain handoff without a reminder stashes the create-a-tickle offer and no prompt`() = runTest {
        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        // No mark-done prompt (nothing to complete)...
        assertNull(store.pending.value)
        // ...but a "create a tickle for Alice?" offer instead (TIC-86).
        val offer = offerStore.pending.value
        assertEquals(1L, offer?.contactId)
        assertEquals("Alice Smith", offer?.contactName)
    }

    @Test
    fun `plain handoff skips the offer when the recipient already has an active tickle`() = runTest {
        // Alice already has an ACTIVE reminder — just not one this compose
        // carried (no attachReminder). Offering would invite a duplicate.
        tickleDao.reminders.add(
            TickleReminder(id = 20L, contactId = 1L, status = TickleStatus.ACTIVE.name)
        )

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        // Outcome 3: no mark-done prompt AND no offer — she's already covered.
        assertNull(store.pending.value)
        assertNull(offerStore.pending.value)
    }

    @Test
    fun `plain handoff skips the offer when the recipient already has a snoozed tickle`() = runTest {
        // SNOOZED is still a live reminder (a date shift, not a mute — TIC-61).
        tickleDao.reminders.add(
            TickleReminder(id = 21L, contactId = 1L, status = TickleStatus.SNOOZED.name)
        )

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
        assertNull(offerStore.pending.value)
    }

    @Test
    fun `plain handoff still offers when the recipient has only completed tickles`() = runTest {
        // A COMPLETED one-time tickle is history, not coverage — offer away.
        tickleDao.reminders.add(
            TickleReminder(id = 22L, contactId = 1L, status = TickleStatus.COMPLETED.name)
        )

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
        assertEquals(1L, offerStore.pending.value?.contactId)
    }

    @Test
    fun `handoff offers a tickle when the attached reminder was already completed`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.COMPLETED.name)
        )
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
        assertEquals(1L, offerStore.pending.value?.contactId)
    }

    @Test
    fun `handoff offers a tickle when the attached reminder was deleted`() = runTest {
        // Reminder 10 is attached but absent from the DB (deleted in the meantime).
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
        assertEquals(1L, offerStore.pending.value?.contactId)
    }

    @Test
    fun `handoff offers a tickle for the actual recipient when re-targeted`() = runTest {
        tickleDao.reminders.add(
            TickleReminder(id = 10L, contactId = 1L, status = TickleStatus.ACTIVE.name)
        )
        // Attached for Alice's reminder, but the message is actually sent to Bob.
        viewModel.attachReminder(reminderId = 10L, contactId = 1L)

        viewModel.recordHandoff(bob)
        advanceUntilIdle()

        // Alice's reminder is not marked done (wrong recipient)...
        assertNull(store.pending.value)
        // ...and the offer targets Bob, the contact actually texted.
        assertEquals(2L, offerStore.pending.value?.contactId)
        assertEquals("Bob Jones", offerStore.pending.value?.contactName)
    }

    @Test
    fun `attachReminder treats -1 sentinel as no reminder and offers a tickle`() = runTest {
        viewModel.attachReminder(reminderId = -1L, contactId = -1L)

        viewModel.recordHandoff(alice)
        advanceUntilIdle()

        assertNull(store.pending.value)
        assertEquals(1L, offerStore.pending.value?.contactId)
    }

    // ---- TIC-90: template apply / replace-draft wiring ------------------------

    private val checkingIn = MessageTemplate(id = 1L, title = "Checking in", body = "Hey! Just checking in.")
    private val runningLate = MessageTemplate(id = 2L, title = "Running late", body = "Running a few minutes late!")

    @Test
    fun `an empty draft never needs a replace confirmation`() {
        assertFalse(viewModel.shouldConfirmTemplateReplace())
    }

    @Test
    fun `hand-typed text needs a replace confirmation before a template applies`() {
        viewModel.setMessage("Hand-typed note that was never a template.")
        assertTrue(viewModel.shouldConfirmTemplateReplace())
    }

    @Test
    fun `applying a template then re-applying it needs no confirmation`() {
        viewModel.applyTemplate(checkingIn)
        assertEquals(checkingIn.body, viewModel.messageBody.value)
        // Untouched since applying — a second template tap is safe to apply directly.
        assertFalse(viewModel.shouldConfirmTemplateReplace())
    }

    @Test
    fun `editing an applied template's body needs a confirmation before a new template applies`() {
        viewModel.applyTemplate(checkingIn)
        viewModel.setMessage(checkingIn.body + " Extra note.")
        assertTrue(viewModel.shouldConfirmTemplateReplace())
    }

    @Test
    fun `applying a second template after confirming updates the tracked applied body`() {
        viewModel.applyTemplate(checkingIn)
        viewModel.applyTemplate(runningLate)
        assertEquals(runningLate.body, viewModel.messageBody.value)
        assertFalse(viewModel.shouldConfirmTemplateReplace())
    }

    @Test
    fun `clearCompose resets the applied-template tracking`() {
        viewModel.applyTemplate(checkingIn)
        viewModel.clearCompose()
        assertEquals("", viewModel.messageBody.value)
        assertFalse(viewModel.shouldConfirmTemplateReplace())
    }
}
