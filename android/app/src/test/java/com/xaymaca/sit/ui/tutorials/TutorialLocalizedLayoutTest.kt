package com.xaymaca.sit.ui.tutorials

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.theme.SITTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class TutorialLocalizedLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test @Config(qualifiers = "de") fun germanGuideAtLargeText() = checkGuide()
    @Test @Config(qualifiers = "ja") fun japaneseGuideAtLargeText() = checkGuide()
    @Test @Config(qualifiers = "ar-ldrtl") fun arabicGuideAtLargeText() = checkGuide()

    private fun checkGuide() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        var actions = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.8f)) {
                SITTheme { TutorialDialog(TutorialTopic.Tickle, onDismiss = {}, onAction = { actions++ }) }
            }
        }
        compose.onNodeWithTag("tutorial.next").assertIsDisplayed()
        compose.onNodeWithText(c.getString(R.string.tutorial_view_screenshot)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(c.getString(R.string.tutorial_close_screenshot)).assertIsDisplayed().performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals(c.getString(R.string.tutorial_progress, 1, 3))
        compose.onNodeWithTag("tutorial.next").performClick()
        compose.onNodeWithTag("tutorial.back").assertIsDisplayed().performClick()
        compose.onNodeWithTag("tutorial.progress").assertTextEquals(c.getString(R.string.tutorial_progress, 1, 3))
        repeat(2) { compose.onNodeWithTag("tutorial.next").performClick() }
        compose.onNodeWithTag("tutorial.next").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, actions) }
    }
}
