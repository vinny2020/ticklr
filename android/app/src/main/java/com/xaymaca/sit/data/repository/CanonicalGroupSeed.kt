package com.xaymaca.sit.data.repository

import android.content.Context
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.ui.theme.WarmCategory

/**
 * Seeds the 5 canonical relationship groups (Family / Close Friends /
 * Work / Milestones / Neighbors & Community) on first launch.
 *
 * Per the warm-redesign decision "categories ARE groups", each canonical
 * category is a real `ContactGroup` identified by its stable
 * `categoryId` string (see WarmCategory). This service keeps those rows
 * present:
 *   - If a row with the canonical categoryId already exists → leave it
 *     alone.
 *   - Else if the user already has a group whose name (case-insensitive)
 *     matches the canonical localized name → adopt it: stamp the
 *     existing row's categoryId. Their existing contacts in that group
 *     are preserved.
 *   - Else → insert a new ContactGroup with the canonical categoryId,
 *     localized name, and category emoji.
 *
 * Idempotent — safe to run on every launch. No SharedPreferences flag.
 */
object CanonicalGroupSeed {

    suspend fun seedIfNeeded(
        dao: ContactGroupDao,
        context: Context,
    ) {
        for (category in WarmCategory.values()) {
            if (dao.getByCategoryId(category.id) != null) continue

            val localizedName = context.getString(category.groupNameRes)
            val collision = dao.findByNameCaseInsensitive(localizedName)
            if (collision != null) {
                dao.update(collision.copy(categoryId = category.id))
                continue
            }

            dao.insert(
                ContactGroup(
                    name = localizedName,
                    emoji = category.defaultEmoji,
                    categoryId = category.id,
                )
            )
        }
    }
}
