package com.xaymaca.sit

import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.service.ImportResult
import com.xaymaca.sit.service.formatImportSummaryMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * TIC-85: the onboarding Import screen now auto-advances to Network on a
 * successful import instead of showing an in-place success card + "Continue"
 * button — the count/duplicates summary travels with the navigation via
 * PendingSnackbarMessageStore instead. [formatImportSummaryMessage] is the
 * pure formatting logic behind that summary; this runs it against the real
 * (English) string resources under Robolectric so plural-category selection
 * is exercised for real, not just asserted against a mocked Context.
 */
@RunWith(RobolectricTestRunner::class)
class ImportSummaryMessageTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `several new contacts and no duplicates reports just the count`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 5, skipped = 0))
        assertEquals("Imported 5 contacts", message)
    }

    @Test
    fun `a single new contact uses the singular plural form`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 1, skipped = 0))
        assertEquals("Imported 1 contact", message)
    }

    @Test
    fun `new contacts plus duplicates reports both counts`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 5, skipped = 2))
        assertEquals("Imported 5 contacts — 2 duplicates skipped", message)
    }

    @Test
    fun `a single new contact plus a single duplicate uses singular forms for both clauses`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 1, skipped = 1))
        assertEquals("Imported 1 contact — 1 duplicate skipped", message)
    }

    @Test
    fun `zero new contacts with duplicates reports an honest no-new-contacts message`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 0, skipped = 3))
        assertEquals("No new contacts — 3 duplicates skipped", message)
    }

    @Test
    fun `zero new contacts with a single duplicate uses the singular duplicate form`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 0, skipped = 1))
        assertEquals("No new contacts — 1 duplicate skipped", message)
    }

    @Test
    fun `an entirely empty import source reports no new contacts without a duplicates clause`() {
        val message = formatImportSummaryMessage(context, ImportResult(inserted = 0, skipped = 0))
        assertEquals("No new contacts found", message)
    }
}
