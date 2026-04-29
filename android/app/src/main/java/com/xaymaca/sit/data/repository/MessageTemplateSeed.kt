package com.xaymaca.sit.data.repository

import android.content.SharedPreferences
import com.xaymaca.sit.data.model.MessageTemplate

object MessageTemplateSeed {
    const val PREFS_KEY_HAS_SEEDED = "hasSeededDefaultTemplates"
    const val DEFAULT_TITLE = "Checking in"
    const val DEFAULT_BODY = "Hey! Just checking in — hope you're doing well. Let's catch up soon!"

    suspend fun seedDefaultIfNeeded(
        repo: MessageTemplateRepository,
        prefs: SharedPreferences
    ) {
        if (prefs.getBoolean(PREFS_KEY_HAS_SEEDED, false)) return
        repo.insertTemplate(MessageTemplate(title = DEFAULT_TITLE, body = DEFAULT_BODY))
        prefs.edit().putBoolean(PREFS_KEY_HAS_SEEDED, true).apply()
    }
}
