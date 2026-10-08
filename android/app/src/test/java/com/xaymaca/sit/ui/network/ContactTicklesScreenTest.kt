package com.xaymaca.sit.ui.network

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.ui.groups.GroupViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class ContactTicklesScreenTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createComposeRule()

    private fun assertVisible(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    @Test
    fun profileDisplaysNewAndUpdatedTicklesAndSwitchesToAnotherPerson() {
        val alice = Contact(id = 1, firstName = "Alice", lastName = "Doe")
        val bob = Contact(id = 2, firstName = "Bob", lastName = "Doe")
        val aliceTickles = MutableStateFlow(emptyList<TickleReminder>())
        val bobTickles = MutableStateFlow(emptyList<TickleReminder>())
        val selected = mutableStateOf(1L)
        val network = mockk<NetworkViewModel>(relaxed = true)
        val groups = mockk<GroupViewModel>(relaxed = true)
        coEvery { network.getContactById(1) } returns alice
        coEvery { network.getContactById(2) } returns bob
        every { network.scheduledTicklesForContact(1) } returns aliceTickles
        every { network.scheduledTicklesForContact(2) } returns bobTickles
        every { groups.groups } returns MutableStateFlow(emptyList())
        every { groups.getGroupsForContact(any()) } returns MutableStateFlow(emptyList())
        composeRule.setContent {
            ContactDetailScreen(contactId = selected.value, onBack = {}, onAddTickle = {}, onEdit = {},
                                viewModel = network, groupViewModel = groups)
        }
        assertVisible("No tickles scheduled for this person.")
        val reminder = TickleReminder(id = 10, contactId = 1, note = "Coffee with Alice", frequency = TickleFrequency.CUSTOM.name,
                                      customIntervalDays = 10, status = TickleStatus.SNOOZED.name)
        composeRule.runOnIdle { aliceTickles.value = listOf(reminder) }
        assertVisible("Coffee with Alice")
        assertVisible("10 days")
        assertVisible("Snoozed")
        composeRule.runOnIdle { aliceTickles.value = listOf(reminder.copy(note = "Updated coffee")) }
        assertVisible("Updated coffee")
        composeRule.onNodeWithText("Coffee with Alice").assertDoesNotExist()
        composeRule.runOnIdle { selected.value = 2 }
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Updated coffee").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Updated coffee").assertDoesNotExist()
        assertVisible("No tickles scheduled for this person.")
        composeRule.runOnIdle { selected.value = 1; aliceTickles.value = emptyList() }
        assertVisible("No tickles scheduled for this person.")
    }
}
