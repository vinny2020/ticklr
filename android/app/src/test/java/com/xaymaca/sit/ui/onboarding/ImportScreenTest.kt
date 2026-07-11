package com.xaymaca.sit.ui.onboarding

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TIC-95: two invisible dead ends in onboarding import.
 *
 *  1. Permanently-denied READ_CONTACTS silently did nothing on tap — no
 *     rationale, no path to Settings. [decideContactsPermissionAction] is
 *     the extracted decision behind the fix, mirroring the same
 *     rationale/permanent-denial logic already proven in
 *     `ui.warm.ContactsAccessBanner`.
 *  2. `GetContent` filtered on a single generic "text" wildcard MIME, which
 *     greyed out CSVs served under a non-text MIME (common from email/Drive
 *     attachments). [CSV_PICKER_MIME_TYPES] is the MIME allow-list now
 *     passed to `OpenDocument` instead.
 */
class ImportScreenTest {

    // --- decideContactsPermissionAction ---

    @Test
    fun `never asked before requests the system prompt`() {
        assertEquals(
            ContactsPermissionAction.RequestPrompt,
            decideContactsPermissionAction(rationaleAllowed = false, userAttemptedOnce = false),
        )
    }

    @Test
    fun `denied once with rationale allowed requests the prompt again`() {
        assertEquals(
            ContactsPermissionAction.RequestPrompt,
            decideContactsPermissionAction(rationaleAllowed = true, userAttemptedOnce = true),
        )
    }

    @Test
    fun `rationale allowed even before any attempt still requests the prompt`() {
        assertEquals(
            ContactsPermissionAction.RequestPrompt,
            decideContactsPermissionAction(rationaleAllowed = true, userAttemptedOnce = false),
        )
    }

    @Test
    fun `permanently denied after an attempt opens settings`() {
        assertEquals(
            ContactsPermissionAction.OpenSettings,
            decideContactsPermissionAction(rationaleAllowed = false, userAttemptedOnce = true),
        )
    }

    // --- CSV_PICKER_MIME_TYPES ---

    @Test
    fun `mime list includes the generic text wildcard for backward compatibility`() {
        assertTrue(CSV_PICKER_MIME_TYPES.contains("text/*"))
    }

    @Test
    fun `mime list covers the common non-text CSV mime types from email and Drive`() {
        val expected = setOf(
            "text/*",
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "application/vnd.ms-excel",
            "application/octet-stream",
        )
        assertEquals(expected, CSV_PICKER_MIME_TYPES.toSet())
    }

    @Test
    fun `mime list has no duplicate entries`() {
        assertEquals(CSV_PICKER_MIME_TYPES.size, CSV_PICKER_MIME_TYPES.toSet().size)
    }
}
