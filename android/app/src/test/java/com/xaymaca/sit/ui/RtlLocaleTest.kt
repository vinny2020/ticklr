package com.xaymaca.sit.ui

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.shared.TicklrToast
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric `qualifiers = "ar"` selects the values-ar/ resource folder and
 * sets the system locale to Arabic — what happens at runtime when a user picks
 * Arabic as their device language. Compose then auto-flips LayoutDirection.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33], qualifiers = "ar")
class RtlLocaleTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun layoutDirectionIsRtlInArabicLocale() {
        var direction: LayoutDirection? = null
        composeRule.setContent {
            direction = LocalLayoutDirection.current
        }
        composeRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun arabicStringResourceLoadsForSettingsTitle() {
        composeRule.setContent {
            Text(text = stringResource(R.string.settings_title))
        }
        composeRule.onNodeWithText("الإعدادات").assertIsDisplayed()
    }

    @Test
    fun ticklrToastRendersArabicCopy() {
        composeRule.setContent {
            TicklrToast(message = "تم حفظ التذكير", onDismiss = {})
        }
        composeRule.onNodeWithText("تم حفظ التذكير").assertIsDisplayed()
    }
}
