package com.xaymaca.sit

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Explicitly invoked on an isolated emulator containing only bundled fake contacts.
 * Captures actual app screens; no images or UI labels are painted over afterwards. */
class TutorialScreenshotCaptureTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private fun text(id: Int): String {
        var value = ""
        scenario.onActivity { value = it.getString(id) }
        return value
    }
    private fun click(id: Int) = compose.onAllNodesWithText(text(id)).onLast().performClick()
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    private fun tab(id: Int) { click(id); compose.waitForIdle() }
    private fun scrollTo(id: Int, towardTop: Boolean = false) {
        val label = text(id)
        repeat(12) {
            if (compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodesWithText(label).onLast().performScrollTo()
                return
            }
            compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { if (towardTop) swipeDown() else swipeUp() }
        }
        error("Cannot find localized control: $label")
    }
    private fun capture(name: String, locale: String) {
        compose.waitForIdle()
        // UiAutomation captures real display frames, outside Compose's test clock.
        // Let navigation/sheet transitions finish before taking the bitmap.
        android.os.SystemClock.sleep(1000)
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.waitForIdleSync()
        val file = File(inst.targetContext.getExternalFilesDir(null), "tutorials/$locale/tutorial_android_$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = requireNotNull(inst.uiAutomation.takeScreenshot())
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
    @Test fun captureNativeScreens() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("captureTutorialScreenshots") == "true")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish")) { "Use an isolated emulator, never a personal phone" }
        val locale = requireNotNull(args.getString("locale"))
        inst.targetContext.getSharedPreferences(SITApp.PREFS_NAME, 0).edit()
            .putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).putInt(SITApp.KEY_THEME_MODE, 1)
            .putBoolean("tutorial.dismissed.Phone", true).putBoolean("tutorial.dismissed.LinkedIn", true)
            .putBoolean("tutorial.dismissed.Tickle", true).putBoolean("tutorial.dismissed.Groups", true).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity {
                check(it.resources.configuration.locales[0].language == java.util.Locale.forLanguageTag(locale).language)
            }
            compose.waitForIdle()
            tab(R.string.nav_network)
            compose.waitForIdle()
            android.os.SystemClock.sleep(1000)
            if (compose.onAllNodesWithText("Fatima Al-Hassan").fetchSemanticsNodes().isEmpty()) {
                tab(R.string.nav_settings)
                scrollTo(R.string.settings_debug_load_title)
                click(R.string.settings_debug_load_title)
                android.os.SystemClock.sleep(4000)
                tab(R.string.nav_network)
            }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Fatima Al-Hassan").fetchSemanticsNodes().isNotEmpty() }
            capture("network", locale)
            tab(R.string.nav_settings)
            click(R.string.settings_import_title)
            capture("import", locale)
            back()
            tab(R.string.nav_network)
            compose.onNodeWithText("Fatima Al-Hassan").performClick()
            capture("profile", locale)
            scrollTo(R.string.contact_detail_add_to_group)
            click(R.string.contact_detail_add_to_group)
            capture("membership", locale)
            back()
            // Return to the profile's action row before opening its real editor.
            scrollTo(R.string.warm_contact_createTickle, towardTop = true)
            click(R.string.warm_contact_createTickle)
            capture("tickle", locale)
            scrollTo(R.string.tickle_edit_preset_birthday)
            click(R.string.tickle_edit_preset_birthday)
            capture("birthday", locale)
            back()
            back()
            tab(R.string.nav_groups)
            capture("groups", locale)
            compose.onNodeWithContentDescription(text(R.string.group_list_create_fab)).performClick()
            capture("new_group", locale)
        } finally { scenario.close() }
    }
}
