package com.xaymaca.sit.ui.tickle

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.R
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.PendingSnackbarMessageStore
import com.xaymaca.sit.service.PendingTickleCompletionStore
import com.xaymaca.sit.service.PendingTickleOfferStore
import com.xaymaca.sit.service.TickleScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * TIC-84 invariant: `upsert()` must survive `viewModelScope` cancellation.
 *
 * On the phone flow TickleEditScreen's TickleViewModel is scoped to the
 * tickle_edit NavBackStackEntry, and since TIC-84 the screen calls
 * `onSaved()` → `popBackStack()` immediately after `upsert()` returns. The pop
 * clears the entry's ViewModelStore, cancelling `viewModelScope` while the
 * coroutine may still be suspended in the Room write — without the
 * `NonCancellable` guard inside `upsert()`, that intermittently loses the save,
 * the alarm sync, and the confirmation snackbar. This test models exactly that:
 * a slow repository write, scope cancelled right after `upsert()` returns
 * (as the pop does), and asserts the write and snackbar post still land.
 *
 * Main is an [UnconfinedTestDispatcher] to mirror production's Main.immediate:
 * the launched body runs synchronously up to its first suspension point, so
 * the NonCancellable context is entered before `upsert()` returns — i.e.
 * before the pop's cancellation can ever be observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TickleViewModelUpsertCancellationTest {

    private val tickleRepository: TickleRepository = mockk()
    private val contactRepository: ContactRepository = mockk()
    private val contactGroupDao: ContactGroupDao = mockk()
    private val context: Context = mockk()
    private val snackbarStore = PendingSnackbarMessageStore()

    private val reminder = TickleReminder(
        id = 0L,
        contactId = null,
        groupId = null,
        note = "ping",
        frequency = "MONTHLY",
        customIntervalDays = null,
        startDate = 0L,
        nextDueDate = 0L,
    )

    @Before
    fun setUp() {
        // Cold-flow stubs consumed eagerly by the ViewModel's stateIn pipelines.
        every { tickleRepository.getAllReminders() } returns flowOf(emptyList())
        every { contactRepository.getAllContacts() } returns flowOf(emptyList())
        every { contactRepository.getAllGroups() } returns flowOf(emptyList())
        every { contactGroupDao.getAllCrossRefs() } returns flowOf(emptyList())
        every { context.getString(R.string.tickle_saved) } returns "Tickle saved"
        every { context.getString(R.string.tickle_notification_contact_fallback) } returns "Someone"
        mockkObject(TickleScheduler)
        justRun { TickleScheduler.syncAlarm(any(), any(), any()) }
        justRun { TickleScheduler.scheduleWorker(any()) }
    }

    @After
    fun tearDown() {
        unmockkObject(TickleScheduler)
        Dispatchers.resetMain()
    }

    @Test
    fun `upsert completes the write and posts the snackbar even when the scope is cancelled by the immediate pop`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        // Slow write: still suspended in Room when the pop lands.
        coEvery { tickleRepository.upsertReminder(any()) } coAnswers {
            delay(500)
            7L
        }
        val viewModel = TickleViewModel(
            tickleRepository = tickleRepository,
            contactRepository = contactRepository,
            contactGroupDao = contactGroupDao,
            pendingTickleCompletionStore = PendingTickleCompletionStore(),
            pendingTickleOfferStore = PendingTickleOfferStore(),
            pendingSnackbarMessageStore = snackbarStore,
            context = context,
        )

        viewModel.upsert(reminder, isNew = true)
        // Nothing has landed yet — we're suspended inside the repository write.
        assertNull(snackbarStore.pending.value)

        // What popBackStack() does to a destination-scoped ViewModel: the
        // entry's ViewModelStore is cleared and viewModelScope is cancelled
        // while the save coroutine is still in flight.
        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        // The NonCancellable block must have carried the whole save unit
        // through the teardown: Room write, alarm sync, worker scheduling,
        // and the app-scoped confirmation.
        coVerify(exactly = 1) { tickleRepository.upsertReminder(reminder) }
        coVerify(exactly = 1) { TickleScheduler.syncAlarm(context, reminder.copy(id = 7L), "Someone") }
        coVerify(exactly = 1) { TickleScheduler.scheduleWorker(context) }
        assertEquals("Tickle saved", snackbarStore.pending.value)
    }
}
