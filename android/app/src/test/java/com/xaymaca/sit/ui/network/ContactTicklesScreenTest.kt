package com.xaymaca.sit.ui.network

import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.xaymaca.sit.ui.nav.Screen
import com.xaymaca.sit.ui.tickle.TickleEditScreen
import com.xaymaca.sit.ui.tickle.TickleViewModel
import org.junit.Assert.assertEquals
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
        composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText(text))
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
            ContactDetailScreen(contactId = selected.value, onBack = {}, onAddTickle = {}, onEdit = {}, onEditTickle = {},
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

    @Test
    fun tappingTickleEditsExistingReminderAndSaveOrBackReturnsToProfile() {
        val contact = Contact(id = 1, firstName = "Alice", lastName = "Doe")
        val due = System.currentTimeMillis() + 20 * 86_400_000L
        val original = TickleReminder(id = 42, contactId = contact.id, note = "Coffee with Alice", startDate = due, nextDueDate = due)
        val tickles = MutableStateFlow(listOf(original))
        val network = mockk<NetworkViewModel>(relaxed = true)
        val groups = mockk<GroupViewModel>(relaxed = true)
        val editor = mockk<TickleViewModel>(relaxed = true)
        coEvery { network.getContactById(contact.id) } returns contact
        every { network.filteredContacts } returns MutableStateFlow(listOf(contact))
        every { network.scheduledTicklesForContact(contact.id) } returns tickles
        every { groups.groups } returns MutableStateFlow(emptyList())
        every { groups.getGroupsForContact(any()) } returns MutableStateFlow(emptyList())
        coEvery { editor.getReminderById(42) } answers { tickles.value.single() }
        every { editor.upsert(any(), isNew = false) } answers {
            tickles.value = listOf(firstArg())
        }
        composeRule.setContent {
            val navigator = rememberNavController()
            NavHost(navController = navigator, startDestination = "profile") {
                composable("profile") {
                    ContactDetailScreen(
                        contactId = contact.id, onBack = {}, onAddTickle = {}, onEdit = {},
                        onEditTickle = { id -> navigator.navigate(Screen.TickleEdit.createRoute(id)) },
                        viewModel = network, groupViewModel = groups,
                    )
                }
                composable(
                    Screen.TickleEdit.ROUTE,
                    arguments = listOf(
                        navArgument("tickleId") { type = NavType.LongType },
                        navArgument("contactId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("groupId") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    TickleEditScreen(
                        tickleId = entry.arguments!!.getLong("tickleId"),
                        onSaved = { navigator.popBackStack() }, onBack = { navigator.popBackStack() },
                        tickleViewModel = editor, networkViewModel = network,
                    )
                }
            }
        }
        assertVisible(original.note)
        composeRule.onNodeWithText(original.note).performClick()
        composeRule.onNodeWithText("Edit Tickle").assertIsDisplayed()
        assertVisible(original.note)
        composeRule.onNodeWithText(original.note).performTextReplacement("Changed coffee plan")
        composeRule.onNodeWithText("Save").performClick()
        assertVisible("Changed coffee plan")
        assertEquals(42L, tickles.value.single().id)
        assertEquals(due, tickles.value.single().nextDueDate)
        composeRule.onNodeWithText("Edit Tickle").assertDoesNotExist()

        composeRule.onNodeWithText("Changed coffee plan").performClick()
        composeRule.onNodeWithText("Edit Tickle").assertIsDisplayed()
        assertVisible("Changed coffee plan")
        composeRule.onNodeWithText("Changed coffee plan").performTextReplacement("Unsaved draft")
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertVisible("Changed coffee plan")
        composeRule.onNodeWithText("Unsaved draft").assertDoesNotExist()
        assertEquals(contact.id, tickles.value.single().contactId)
    }
}
