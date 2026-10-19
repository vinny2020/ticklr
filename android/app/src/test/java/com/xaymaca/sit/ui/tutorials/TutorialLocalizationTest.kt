package com.xaymaca.sit.ui.tutorials

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class TutorialLocalizationTest {
    private val languages = listOf("en", "ar", "cs", "de", "el", "es", "fr", "he", "hi", "hu", "it", "ja", "ko", "nl", "pl", "pt", "ro", "ru", "sv", "ur", "zh-Hans")
    private fun context(language: String): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(app.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return app.createConfigurationContext(config)
    }

    @Test fun tutorialCopyAndProgressResolveInEverySupportedLanguage() {
        val english = context("en").getString(R.string.tutorial_phone_1_body)
        for (language in languages) {
            val c = context(language)
            TutorialTopic.entries.forEach { topic ->
                assertTrue(c.getString(topic.title).isNotBlank(), language)
                topic.pages.forEach { page ->
                    assertTrue(c.getString(page.title).isNotBlank(), language)
                    assertTrue(c.getString(page.body).isNotBlank(), language)
                }
            }
            if (language != "en") assertNotEquals(english, c.getString(R.string.tutorial_phone_1_body), language)
            assertTrue(c.getString(R.string.tutorial_tickle_1_body).contains(c.getString(R.string.warm_contact_createTickle)), "$language: instructions must name the real action")
            assertTrue(c.getString(R.string.tutorial_groups_2_body).contains(c.getString(R.string.contact_detail_add_to_group)), "$language: group action must match the screenshot")
            val progress = c.getString(R.string.tutorial_progress, 7, 13)
            val locale = c.resources.configuration.locales[0]
            assertTrue(progress.contains(String.format(locale, "%d", 7)) && progress.contains(String.format(locale, "%d", 13)), language)
            assertFalse(progress.contains("%"), language)
            assertFalse(c.getString(R.string.tutorial_language).contains("English"), language)
        }
    }

    @Test fun screenshotResourcesUseTheSameLanguageAsTheTutorial() {
        for (language in languages) {
            val c = context(language)
            val expected = when (language) { "en" -> "drawable-nodpi"; "zh-Hans" -> "drawable-zh-nodpi"; "he" -> "drawable-iw-nodpi"; else -> "drawable-$language-nodpi" }
            TutorialTopic.entries.flatMap { it.pages }.map { it.screenshot }.distinct().forEach { id ->
                val value = TypedValue()
                c.resources.getValue(id, value, true)
                val path = value.string.toString()
                assertTrue(path.contains(expected) || (language == "he" && path.contains("drawable-he-nodpi")), "$language: $path")
            }
        }
        // An unsupported locale must have a complete, usable English fallback.
        assertEquals(context("en").getString(R.string.tutorial_phone_1_body), context("bg").getString(R.string.tutorial_phone_1_body))
    }

    @Test fun changingLanguageDoesNotResetDismissalHistory() {
        val english = context("en")
        TutorialHistory(english).dismiss(TutorialTopic.Phone)
        for (language in languages) assertTrue(TutorialHistory(context(language)).isDismissed(TutorialTopic.Phone), language)
    }
}
