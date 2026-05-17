package com.xaymaca.sit.data.repository

import android.content.SharedPreferences
import com.xaymaca.sit.data.model.MessageTemplate

object MessageTemplateSeed {
    const val PREFS_KEY_HAS_SEEDED = "hasSeededDefaultTemplates"
    const val DEFAULT_TITLE = "Checking in"
    const val DEFAULT_BODY = "Hey! Just checking in — hope you're doing well. Let's catch up soon!"

    /**
     * Inserts the default "Checking in" template when the templates
     * table is empty. Previously gated only by a SharedPrefs flag,
     * which left users stuck with no default in two scenarios:
     *   1. Prefs flag was set in a prior install but the row got
     *      wiped (Clear All Data, reinstall race, etc.)
     *   2. A previous bug set the flag without actually inserting
     *
     * Now DB-aware: if the table is empty, the default lands —
     * regardless of the prefs flag. We still update the flag so any
     * older code paths reading it remain consistent. Tradeoff: a user
     * who explicitly deletes the default will see it come back on
     * next launch; acceptable since the default is genuinely useful
     * and a re-delete is one tap.
     */
    suspend fun seedDefaultIfNeeded(
        repo: MessageTemplateRepository,
        prefs: SharedPreferences
    ) {
        val existing = repo.count()
        if (existing > 0) {
            // User already has templates — mark flag for legacy consistency,
            // skip the insert.
            if (!prefs.getBoolean(PREFS_KEY_HAS_SEEDED, false)) {
                prefs.edit().putBoolean(PREFS_KEY_HAS_SEEDED, true).apply()
            }
            return
        }
        repo.insertTemplate(MessageTemplate(title = DEFAULT_TITLE, body = DEFAULT_BODY))
        prefs.edit().putBoolean(PREFS_KEY_HAS_SEEDED, true).apply()
    }
}
