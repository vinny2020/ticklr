package com.xaymaca.sit.ui.tickle

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.ui.network.NetworkViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Screen-level wiring tests for `TickleEditScreen`.
 *
 * These verify that the right `nextDueDate` actually flows from form state
 * through the save handler into `TickleViewModel.upsert(...)`. The pure
 * decision logic is already covered by `TickleSchedulerTest`; these tests
 * exist to catch regressions where someone bypasses the helper, passes the
 * wrong arguments, or forgets to look up the original reminder.
 *
 * The screen accepts both ViewModels as composable parameters with
 * `hiltViewModel()` defaults, so tests pass mocks directly and skip Hilt's
 * VM-injection path entirely. The `HiltAndroidRule` is kept so this file
 * also serves as the template for tests that DO need real Hilt bindings.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class TickleEditScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createComposeRule()

    private lateinit var tickleVM: TickleViewModel
    private lateinit var networkVM: NetworkViewModel

    private val contact = Contact(
        id = 1L,
        firstName = "Alice",
        lastName = "Doe",
        phoneNumbers = """["555-1234"]""",
        emails = """["alice@example.com"]""",
        company = "Acme",
        fingerprint = "alice"
    )

    @Before
    fun setUp() {
        tickleVM = mockk(relaxed = true)
        networkVM = mockk(relaxed = true)
        every { tickleVM.toastMessage } returns MutableStateFlow(null)
        every { networkVM.filteredContacts } returns MutableStateFlow(listOf(contact))
        coEvery { networkVM.getContactById(contact.id) } returns contact
    }

    @Test
    fun editingNoteOnly_preservesNextDueDate() {
        val futureDue = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(20)
        val original = TickleReminder(
            id = 42L,
            contactId = contact.id,
            note = "before",
            frequency = TickleFrequency.MONTHLY.name,
            customIntervalDays = null,
            startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60),
            nextDueDate = futureDue,
            status = TickleStatus.ACTIVE.name
        )
        coEvery { tickleVM.getReminderById(42L) } returns original

        composeRule.setContent {
            TickleEditScreen(
                tickleId = 42L,
                onSaved = {},
                onBack = {},
                tickleViewModel = tickleVM,
                networkViewModel = networkVM
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        val saved = slot<TickleReminder>()
        coVerify { tickleVM.upsert(capture(saved), isNew = false) }
        assertEquals(
            "Note-only edit must preserve the original nextDueDate",
            futureDue, saved.captured.nextDueDate
        )
        assertEquals(TickleFrequency.MONTHLY.name, saved.captured.frequency)
    }

    @Test
    fun changingFrequency_recomputesFromNow() {
        val original = TickleReminder(
            id = 42L,
            contactId = contact.id,
            note = "ping",
            frequency = TickleFrequency.WEEKLY.name,
            customIntervalDays = null,
            startDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60),
            // Far-future original due — if the screen wrongly preserved it,
            // the assertion below would fail by a wide margin.
            nextDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365),
            status = TickleStatus.ACTIVE.name
        )
        coEvery { tickleVM.getReminderById(42L) } returns original

        composeRule.setContent {
            TickleEditScreen(
                tickleId = 42L,
                onSaved = {},
                onBack = {},
                tickleViewModel = tickleVM,
                networkViewModel = networkVM
            )
        }

        composeRule.waitForIdle()

        // Open the frequency dropdown by clicking the current selection ("Weekly"),
        // then pick "Monthly". "Monthly" only exists inside the open menu, so the
        // text match is unambiguous.
        composeRule.onNodeWithText("Weekly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Monthly").performClick()
        composeRule.waitForIdle()

        val saveTimestamp = System.currentTimeMillis()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        val saved = slot<TickleReminder>()
        coVerify { tickleVM.upsert(capture(saved), isNew = false) }
        assertEquals(TickleFrequency.MONTHLY.name, saved.captured.frequency)
        // Calendar +1 month from `saveTimestamp` ≈ 28–31 days; allow a wide
        // tolerance so the test isn't sensitive to month length or DST.
        val approxOneMonth = TimeUnit.DAYS.toMillis(30)
        val delta = abs(saved.captured.nextDueDate - (saveTimestamp + approxOneMonth))
        assertTrue(
            "Expected nextDueDate ≈ now + 1 month after frequency change, was off by ${delta}ms",
            delta < TimeUnit.DAYS.toMillis(3)
        )
    }

    @Test
    fun creatingWeekly_setsNextDueDateOneWeekAhead() {
        composeRule.setContent {
            TickleEditScreen(
                tickleId = null,
                onSaved = {},
                onBack = {},
                tickleViewModel = tickleVM,
                networkViewModel = networkVM
            )
        }

        composeRule.waitForIdle()

        // Pick the contact from the visible list (CREATE-mode shows it until a
        // contact is selected).
        composeRule.onNodeWithText(contact.fullName).performClick()
        composeRule.waitForIdle()

        // Default frequency is MONTHLY. Open dropdown and choose Weekly.
        composeRule.onNodeWithText("Monthly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Weekly").performClick()
        composeRule.waitForIdle()

        val saveTimestamp = System.currentTimeMillis()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        val saved = slot<TickleReminder>()
        coVerify { tickleVM.upsert(capture(saved), isNew = true) }
        assertEquals(TickleFrequency.WEEKLY.name, saved.captured.frequency)
        assertEquals(contact.id, saved.captured.contactId)
        val expected = saveTimestamp + TimeUnit.DAYS.toMillis(7)
        val delta = abs(saved.captured.nextDueDate - expected)
        assertTrue(
            "Expected nextDueDate ≈ now + 7d, was off by ${delta}ms",
            delta < TimeUnit.SECONDS.toMillis(5)
        )
    }

    @Test
    fun creatingWithEmptyNote_substitutesLocalizedDefault() {
        // Empty note → save → reminder.note must be the localized "Stay in
        // touch" default. Resolved via getString so the assertion stays
        // locale-independent in CI.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedDefault = context.getString(R.string.tickle_edit_default_note)

        composeRule.setContent {
            TickleEditScreen(
                tickleId = null,
                onSaved = {},
                onBack = {},
                tickleViewModel = tickleVM,
                networkViewModel = networkVM
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(contact.fullName).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        val saved = slot<TickleReminder>()
        coVerify { tickleVM.upsert(capture(saved), isNew = true) }
        assertEquals(expectedDefault, saved.captured.note)
    }

    @Test
    fun rapidSaveTaps_invokeUpsertExactlyOnce() {
        // Regression for the duplicate-tickle bug. Without the isSaving guard,
        // three rapid taps each launch a fresh insert. With the guard, the
        // first tap flips canSave = false synchronously; subsequent taps land
        // on a disabled button and don't fire. Compose UI test's clock
        // advances synchronously, so this is deterministic.
        composeRule.setContent {
            TickleEditScreen(
                tickleId = null,
                onSaved = {},
                onBack = {},
                tickleViewModel = tickleVM,
                networkViewModel = networkVM
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(contact.fullName).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        coVerify(exactly = 1) { tickleVM.upsert(any(), any()) }
    }
}
