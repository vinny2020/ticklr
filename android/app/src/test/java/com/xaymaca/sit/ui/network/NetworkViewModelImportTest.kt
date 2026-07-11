package com.xaymaca.sit.ui.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.service.ContactImportService
import com.xaymaca.sit.service.ImportResult
import com.xaymaca.sit.service.LinkedInCSVParser
import com.xaymaca.sit.service.PendingSnackbarMessageStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream
import kotlin.test.assertEquals

/**
 * TIC-85: NetworkViewModel.importFromContacts()/importFromCSV() now post the
 * post-import summary to PendingSnackbarMessageStore (TIC-84 pattern) so it
 * survives the Import screen's immediate auto-advance to Network — there is
 * no more on-screen success card sticking around to read the count from.
 * These tests verify both the returned ImportResult tally and the exact
 * message landed in the store, for the plain-success, some-duplicates, and
 * all-duplicates (0 new) cases on both import paths.
 *
 * Runs under Robolectric (real Context, real string resources) rather than
 * mocking Context — formatImportSummaryMessage reads plurals via
 * context.resources.getQuantityString, and mockk can't cheaply stub that
 * chained call the way it stubs a flat context.getString(...).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkViewModelImportTest {

    private val contactRepository: ContactRepository = mockk()
    private val contactImportService: ContactImportService = mockk()
    private val linkedInCSVParser: LinkedInCSVParser = mockk()
    private val contactDao: ContactDao = mockk()
    private val snackbarStore = PendingSnackbarMessageStore()
    private lateinit var context: Context

    private fun buildViewModel(): NetworkViewModel {
        every { contactDao.countContactsInCategory(any()) } returns flowOf(0)
        return NetworkViewModel(
            contactRepository = contactRepository,
            contactImportService = contactImportService,
            linkedInCSVParser = linkedInCSVParser,
            contactDao = contactDao,
            pendingSnackbarMessageStore = snackbarStore,
            context = context,
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- importFromContacts ------------------------------------------------

    @Test
    fun `importFromContacts with only new contacts posts a plain count message`() = runTest {
        coEvery { contactImportService.importPhoneContacts() } returns ImportResult(inserted = 4, skipped = 0)
        val viewModel = buildViewModel()

        val result = viewModel.importFromContacts()

        assertEquals(ImportResult(4, 0), result)
        assertEquals("Imported 4 contacts", snackbarStore.pending.value)
    }

    @Test
    fun `importFromContacts with some duplicates posts the combined message`() = runTest {
        coEvery { contactImportService.importPhoneContacts() } returns ImportResult(inserted = 4, skipped = 2)
        val viewModel = buildViewModel()

        val result = viewModel.importFromContacts()

        assertEquals(ImportResult(4, 2), result)
        assertEquals("Imported 4 contacts — 2 duplicates skipped", snackbarStore.pending.value)
    }

    @Test
    fun `importFromContacts with all duplicates posts the honest no-new-contacts message`() = runTest {
        coEvery { contactImportService.importPhoneContacts() } returns ImportResult(inserted = 0, skipped = 5)
        val viewModel = buildViewModel()

        val result = viewModel.importFromContacts()

        assertEquals(ImportResult(0, 5), result)
        assertEquals("No new contacts — 5 duplicates skipped", snackbarStore.pending.value)
    }

    // -- importFromCSV -------------------------------------------------------

    private val alice = Contact(firstName = "Alice", lastName = "Anders")
    private val bob = Contact(firstName = "Bob", lastName = "Brown")
    private val carol = Contact(firstName = "Carol", lastName = "Chu")
    private val inputStream: InputStream = mockk()

    @Test
    fun `importFromCSV tallies genuine inserts separately from duplicates and posts the combined message`() = runTest {
        every { linkedInCSVParser.parse(inputStream) } returns listOf(alice, bob, carol)
        coEvery { contactRepository.insertContact(alice) } returns 1L
        coEvery { contactRepository.insertContact(bob) } returns -1L // duplicate
        coEvery { contactRepository.insertContact(carol) } returns 3L
        val viewModel = buildViewModel()

        val result = viewModel.importFromCSV(inputStream)

        assertEquals(ImportResult(inserted = 2, skipped = 1), result)
        assertEquals("Imported 2 contacts — 1 duplicate skipped", snackbarStore.pending.value)
    }

    @Test
    fun `importFromCSV of entirely duplicate rows posts the all-duplicates message`() = runTest {
        every { linkedInCSVParser.parse(inputStream) } returns listOf(alice, bob)
        coEvery { contactRepository.insertContact(alice) } returns -1L
        coEvery { contactRepository.insertContact(bob) } returns -1L
        val viewModel = buildViewModel()

        val result = viewModel.importFromCSV(inputStream)

        assertEquals(ImportResult(inserted = 0, skipped = 2), result)
        assertEquals("No new contacts — 2 duplicates skipped", snackbarStore.pending.value)
    }

    @Test
    fun `importFromCSV of an empty file posts the empty-source message with no duplicates clause`() = runTest {
        every { linkedInCSVParser.parse(inputStream) } returns emptyList()
        val viewModel = buildViewModel()

        val result = viewModel.importFromCSV(inputStream)

        assertEquals(ImportResult(inserted = 0, skipped = 0), result)
        assertEquals("No new contacts found", snackbarStore.pending.value)
    }
}
