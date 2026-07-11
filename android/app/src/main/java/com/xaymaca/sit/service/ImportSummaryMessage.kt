package com.xaymaca.sit.service

import android.content.Context
import com.xaymaca.sit.R

/**
 * Formats the post-import summary posted to [PendingSnackbarMessageStore] once
 * the onboarding Import screen (or a mid-session import from Settings/Network)
 * completes and auto-advances to the Network tab (TIC-85). Previously import
 * success surfaced an in-place card + a "Continue" button that only echoed
 * work the import had already done; the count now travels with the
 * navigation instead of requiring an extra tap to see it.
 *
 * Pure function of [ImportResult] (plus the [Context] needed to read string
 * resources) so it's unit-testable without a live import path — see
 * ImportSummaryMessageTest.
 *
 * Four cases:
 *  - inserted > 0, skipped == 0 → "Imported N contact(s)"
 *  - inserted > 0, skipped > 0  → "Imported N contact(s) — M duplicate(s) skipped"
 *  - inserted == 0, skipped > 0 → "No new contacts — M duplicate(s) skipped"
 *  - inserted == 0, skipped == 0 → "No new contacts found" (empty source, e.g. an
 *    empty CSV — not explicitly a duplicates case, so no dash clause)
 */
fun formatImportSummaryMessage(context: Context, result: ImportResult): String {
    val duplicatesPart = if (result.skipped > 0) {
        context.resources.getQuantityString(
            R.plurals.import_result_duplicates_skipped, result.skipped, result.skipped
        )
    } else null

    return if (result.inserted > 0) {
        val contactsPart = context.resources.getQuantityString(
            R.plurals.import_result_contacts, result.inserted, result.inserted
        )
        if (duplicatesPart != null) {
            context.getString(R.string.import_result_combined, contactsPart, duplicatesPart)
        } else {
            contactsPart
        }
    } else if (duplicatesPart != null) {
        context.getString(R.string.import_result_no_new_with_duplicates, duplicatesPart)
    } else {
        context.getString(R.string.import_result_no_new_contacts)
    }
}
