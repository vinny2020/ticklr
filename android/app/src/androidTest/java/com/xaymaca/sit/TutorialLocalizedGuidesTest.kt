package com.xaymaca.sit

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.xaymaca.sit.ui.tutorials.TutorialTopic
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Opt-in real-device layout check, restricted to the disposable emulator. */
class TutorialLocalizedGuidesTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun checkTranslatedGuides() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("validateTutorialLocalization") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.targetContext.getSharedPreferences(SITApp.PREFS_NAME, 0).edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        fun text(id: Int, vararg values: Any): String {
            var result = ""
            scenario.onActivity { result = it.getString(id, *values) }
            return result
        }
        try {
            scenario.onActivity { check(it.resources.configuration.locales[0].language == java.util.Locale.forLanguageTag(args.getString("locale")).language) }
            compose.onAllNodesWithText(text(R.string.nav_settings)).onLast().performClick()
            compose.onNodeWithText(text(R.string.tutorial_library)).performScrollTo().performClick()
            for (topic in TutorialTopic.entries) {
                compose.onNodeWithText(text(topic.title)).performClick()
                val next = compose.onNodeWithTag("tutorial.next")
                next.assertIsDisplayed()
                compose.onNodeWithText(text(R.string.tutorial_view_screenshot)).performScrollTo().performClick()
                compose.onNodeWithContentDescription(text(R.string.tutorial_close_screenshot)).assertIsDisplayed().performClick()
                compose.onNodeWithTag("tutorial.progress").assertTextEquals(text(R.string.tutorial_progress, 1, 3))
                android.os.SystemClock.sleep(700)
                val file = File(inst.targetContext.getExternalFilesDir(null), "tutorial-checks/${args.getString("locale")}/${topic.name}.png")
                file.parentFile!!.mkdirs()
                requireNotNull(inst.uiAutomation.takeScreenshot()).let { bitmap ->
                    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) };bitmap.recycle()
                }
                next.performClick()
                compose.onNodeWithTag("tutorial.back").assertIsDisplayed().performClick()
                compose.onNodeWithTag("tutorial.progress").assertTextEquals(text(R.string.tutorial_progress, 1, 3))
                repeat(2) { next.performClick() }
                next.assertIsDisplayed()
                compose.onNodeWithTag("tutorial.close").assertIsDisplayed().performClick()
                compose.onNodeWithText(text(topic.title)).performClick()
                compose.onNodeWithTag("tutorial.progress").assertTextEquals(text(R.string.tutorial_progress, 1, 3))
                compose.onNodeWithTag("tutorial.close").performClick()
            }
        } finally { scenario.close() }
    }
}
