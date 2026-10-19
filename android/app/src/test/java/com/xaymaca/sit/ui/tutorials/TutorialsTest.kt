package com.xaymaca.sit.ui.tutorials

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.ui.nav.tutorialDestination
import com.xaymaca.sit.ui.theme.SITTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class TutorialsTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun resetTutorialHistory() {
        val editor = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE).edit()
        TutorialTopic.entries.forEach { editor.remove("tutorial.dismissed.${it.name}") }
        editor.commit()
    }

    @Test fun dismissedSuggestionsStayDismissedWithoutAffectingOtherGuides() {
        TutorialHistory(context).dismiss(TutorialTopic.Phone)
        assertTrue(TutorialHistory(context).isDismissed(TutorialTopic.Phone))
        assertFalse(TutorialHistory(context).isDismissed(TutorialTopic.LinkedIn))
        assertFalse(TutorialHistory(context).isDismissed(TutorialTopic.Tickle))
    }

    @Test fun closeNeverInvokesTheRealAction() {
        var closed = false
        var actions = 0
        compose.setContent {
            SITTheme { TutorialDialog(TutorialTopic.Tickle, onDismiss = { closed = true }, onAction = { actions++ }) }
        }
        compose.onNodeWithTag("tutorial.close").performClick()
        compose.runOnIdle { assertTrue(closed); assertEquals(0, actions) }
    }

    @Test fun backAndNextReachTheFinalActionWithoutPassingASampleContact() {
        var actions = 0
        compose.setContent {
            SITTheme { TutorialDialog(TutorialTopic.Tickle, onDismiss = {}, onAction = { actions++ }) }
        }
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("1 of 3")
        compose.onNodeWithTag("tutorial.next").performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("2 of 3")
        compose.onNodeWithTag("tutorial.back").performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("1 of 3")
        repeat(2) { compose.onNodeWithTag("tutorial.next").performClick() }
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("3 of 3")
        compose.runOnIdle { assertEquals(0, actions) }
        compose.onNodeWithTag("tutorial.next").performClick()
        compose.runOnIdle { assertEquals(1, actions) }
        assertEquals("tickle_edit/-1", tutorialDestination(TutorialTopic.Tickle))
    }

    @Test fun libraryReplaysGuidesAfterDismissal() {
        TutorialTopic.entries.forEach { TutorialHistory(context).dismiss(it) }
        compose.setContent { SITTheme { TutorialLibraryScreen(onBack = {}, onAction = {}) } }
        compose.onNodeWithTag("tutorial.library.Phone").performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("1 of 3")
        compose.onNodeWithTag("tutorial.close").performClick()
        compose.onNodeWithTag("tutorial.library.Phone").performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("1 of 3")
    }

    @Test fun dismissingSuggestionHidesItAndPersistsTheChoice() {
        compose.setContent { SITTheme { TutorialInvitation(TutorialTopic.Groups, onAction = {}) } }
        compose.onNodeWithTag("tutorial.invitation.Groups").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss tutorial suggestion").performClick()
        compose.onNodeWithTag("tutorial.invitation.Groups").assertDoesNotExist()
        compose.runOnIdle { assertTrue(TutorialHistory(context).isDismissed(TutorialTopic.Groups)) }
    }

    @Test fun screenshotViewerCanCloseWithoutClosingTheGuide() {
        compose.setContent { SITTheme { TutorialDialog(TutorialTopic.Tickle, onDismiss = {}, onAction = {}) } }
        compose.onNodeWithTag("tutorial.screenshot.0").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close screenshot").performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals("1 of 3")
    }

    @Test fun largeTextKeepsTheNavigationVisible() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                SITTheme { TutorialDialog(TutorialTopic.Phone, onDismiss = {}, onAction = {}) }
            }
        }
        compose.onNodeWithTag("tutorial.next").assertIsDisplayed()
        compose.onNodeWithTag("tutorial.close").assertIsDisplayed()
    }

    @Test fun realDestinationsPreserveInAppImportBehavior() {
        assertEquals("import?origin=inApp", tutorialDestination(TutorialTopic.Phone))
        assertEquals("import?origin=inApp", tutorialDestination(TutorialTopic.LinkedIn))
        assertEquals("groups", tutorialDestination(TutorialTopic.Groups))
    }
}
