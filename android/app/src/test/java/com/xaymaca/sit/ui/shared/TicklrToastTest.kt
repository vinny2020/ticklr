package com.xaymaca.sit.ui.shared

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Demonstration screen-level test using Robolectric + Compose UI test + Hilt.
 *
 * This file is the template for future screen tests. The three pieces:
 *   • RobolectricTestRunner — runs Android code on the JVM, no emulator needed.
 *   • createComposeRule() — renders Composables and exposes finders/assertions.
 *   • HiltAndroidRule + HiltTestApplication — wires up DI in tests.
 *
 * For tests that need fake repositories, add to this class (or a copy):
 *   @UninstallModules(DatabaseModule::class)  — replace the real bindings
 *   @InstallIn(SingletonComponent::class) @Module object FakeModule {
 *       @Provides fun fakeContactRepo(): ContactRepository = FakeContactRepository()
 *   }
 *   @Inject lateinit var contactRepository: ContactRepository
 * Then call hiltRule.inject() at the top of each @Test.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class TicklrToastTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun rendersMessageWhenProvided() {
        composeRule.setContent {
            TicklrToast(message = "saved", onDismiss = {})
        }
        composeRule.onNodeWithText("saved").assertIsDisplayed()
    }

    @Test
    fun rendersNothingWhenMessageIsNull() {
        composeRule.setContent {
            TicklrToast(message = null, onDismiss = {})
        }
        composeRule.onNodeWithText("saved").assertDoesNotExist()
    }

    @Test
    fun callsOnDismissAfterTimeout() {
        var dismissed = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TicklrToast(message = "saved", onDismiss = { dismissed = true })
        }
        // TicklrToast schedules onDismiss via LaunchedEffect after 2000ms.
        composeRule.mainClock.advanceTimeBy(2_500)
        composeRule.waitForIdle()
        assert(dismissed) { "onDismiss should have been called after 2s" }
    }
}
