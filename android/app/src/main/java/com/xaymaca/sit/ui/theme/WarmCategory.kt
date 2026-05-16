package com.xaymaca.sit.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.xaymaca.sit.R

// The 5 canonical relationship categories. Per the warm-redesign decision
// "categories ARE groups", each canonical category is identified by a
// stable string `id` that gets persisted on `ContactGroup.categoryId`
// when seeded (see CanonicalGroupSeed). The string is the source of
// truth — Android can't use the iOS UUID-as-primary-key approach
// because the Room `ContactGroup.id` is auto-generated Long.
//
// The `id` strings below MUST NEVER change once shipped — they're how
// every install identifies a canonical group.

enum class WarmCategory(val id: String) {
    Family("family"),
    Friends("friends"),
    Work("work"),
    Milestones("milestones"),
    Community("community");

    /// Stable display order on the Groups list. User-created groups
    /// follow after, sorted by createdAt.
    val sortOrder: Int
        get() = when (this) {
            Family -> 0
            Friends -> 1
            Work -> 2
            Milestones -> 3
            Community -> 4
        }

    /// Default emoji used as a `ContactGroup.emoji` fallback for the
    /// seeded canonical groups. Users can change this in Group edit.
    val defaultEmoji: String
        get() = when (this) {
            Family -> "👨‍👩‍👧"  // 👨‍👩‍👧
            Friends -> "💛"                                       // 💛
            Work -> "💼"                                          // 💼
            Milestones -> "🎂"                                    // 🎂
            Community -> "🏘️"                               // 🏘️
        }

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_label
            Friends -> R.string.warm_category_friends_label
            Work -> R.string.warm_category_work_label
            Milestones -> R.string.warm_category_milestones_label
            Community -> R.string.warm_category_community_label
        }

    @get:StringRes
    val groupNameRes: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_groupName
            Friends -> R.string.warm_category_friends_groupName
            Work -> R.string.warm_category_work_groupName
            Milestones -> R.string.warm_category_milestones_groupName
            Community -> R.string.warm_category_community_groupName
        }

    @get:StringRes
    val headlineLine1Res: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_headline_line1
            Friends -> R.string.warm_category_friends_headline_line1
            Work -> R.string.warm_category_work_headline_line1
            Milestones -> R.string.warm_category_milestones_headline_line1
            Community -> R.string.warm_category_community_headline_line1
        }

    @get:StringRes
    val headlineLine2Res: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_headline_line2
            Friends -> R.string.warm_category_friends_headline_line2
            Work -> R.string.warm_category_work_headline_line2
            Milestones -> R.string.warm_category_milestones_headline_line2
            Community -> R.string.warm_category_community_headline_line2
        }

    @get:StringRes
    val bodyRes: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_body
            Friends -> R.string.warm_category_friends_body
            Work -> R.string.warm_category_work_body
            Milestones -> R.string.warm_category_milestones_body
            Community -> R.string.warm_category_community_body
        }

    @get:StringRes
    val promptRes: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_prompt
            Friends -> R.string.warm_category_friends_prompt
            Work -> R.string.warm_category_work_prompt
            Milestones -> R.string.warm_category_milestones_prompt
            Community -> R.string.warm_category_community_prompt
        }

    @get:StringRes
    val promptShortRes: Int
        get() = when (this) {
            Family -> R.string.warm_category_family_promptShort
            Friends -> R.string.warm_category_friends_promptShort
            Work -> R.string.warm_category_work_promptShort
            Milestones -> R.string.warm_category_milestones_promptShort
            Community -> R.string.warm_category_community_promptShort
        }

    val icon: ImageVector
        get() = when (this) {
            Family -> Icons.Filled.Favorite
            Friends -> Icons.Filled.Star
            Work -> Icons.Filled.Work
            Milestones -> Icons.Filled.CalendarMonth
            Community -> Icons.Filled.Groups
        }

    val palette: WarmCategoryPalette
        get() = when (this) {
            Family -> WarmCategoryPalette(
                accent      = Color(0xFF9C3F3C),
                accentSoft  = Color(0xFFE8C9C4),
                accentTint  = Color(0xFFF4E2DC),
                accentBadge = Color(0xFFF0CCC2),
            )
            Friends -> WarmCategoryPalette(
                accent      = Color(0xFF3F5C7A),
                accentSoft  = Color(0xFFC7D4E0),
                accentTint  = Color(0xFFDCE5ED),
                accentBadge = Color(0xFFD2DDE7),
            )
            Work -> WarmCategoryPalette(
                accent      = Color(0xFF4F6B47),
                accentSoft  = Color(0xFFC7D3BD),
                accentTint  = Color(0xFFDCE4D2),
                accentBadge = Color(0xFFCFDBC4),
            )
            Milestones -> WarmCategoryPalette(
                accent      = Color(0xFFA7791C),
                accentSoft  = Color(0xFFE6D1A0),
                accentTint  = Color(0xFFEDDEB6),
                accentBadge = Color(0xFFE8CF94),
            )
            Community -> WarmCategoryPalette(
                accent      = Color(0xFFB26342),
                accentSoft  = Color(0xFFE8C2AC),
                accentTint  = Color(0xFFF0D4C2),
                accentBadge = Color(0xFFEBC4AE),
            )
        }

    companion object {
        fun from(categoryId: String?): WarmCategory? {
            if (categoryId == null) return null
            return values().firstOrNull { it.id == categoryId }
        }
    }
}

data class WarmCategoryPalette(
    val accent: Color,
    val accentSoft: Color,
    val accentTint: Color,
    val accentBadge: Color,
)
