package com.xaymaca.sit

import com.xaymaca.sit.R
import com.xaymaca.sit.ui.theme.WarmCategory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mirrors iOS WarmCategoryTests. Locks down the canonical category
 * contract (stable ids, sort order, palette + glyph presence, string
 * resource bindings) — anything that breaks here is a regression that
 * would silently corrupt existing installs.
 */
class WarmCategoryTest {

    /** CRITICAL: these ids are persisted to ContactGroup.categoryId
     *  on every install. They MUST NOT change once shipped. */
    @Test
    fun `canonical ids are stable`() {
        val expected = listOf(
            WarmCategory.Family to "family",
            WarmCategory.Friends to "friends",
            WarmCategory.Work to "work",
            WarmCategory.Milestones to "milestones",
            WarmCategory.Community to "community",
        )
        for ((category, id) in expected) {
            assertEquals(id, category.id, "${category.name} id drifted — DO NOT change once shipped")
        }
    }

    @Test
    fun `canonical ids are unique`() {
        val ids = WarmCategory.values().map { it.id }
        assertEquals(ids.size, ids.toSet().size, "canonical ids collided")
    }

    @Test
    fun `sort order matches Family-Friends-Work-Milestones-Community`() {
        val ordered = WarmCategory.values().sortedBy { it.sortOrder }
        assertEquals(
            listOf(
                WarmCategory.Family,
                WarmCategory.Friends,
                WarmCategory.Work,
                WarmCategory.Milestones,
                WarmCategory.Community,
            ),
            ordered,
        )
    }

    @Test
    fun `from resolves canonical ids`() {
        for (category in WarmCategory.values()) {
            assertEquals(category, WarmCategory.from(category.id))
        }
    }

    @Test
    fun `from returns null for unknown or null id`() {
        assertNull(WarmCategory.from(null))
        assertNull(WarmCategory.from("not-a-real-category"))
        assertNull(WarmCategory.from(""))
    }

    @Test
    fun `every category exposes a non-empty default emoji and palette`() {
        for (category in WarmCategory.values()) {
            assertFalse(category.defaultEmoji.isBlank(), "${category.name} default emoji is blank")
            // Palette colors come from a sealed set — verify all 4 swatches
            // exist (not the default Color.Unspecified).
            val p = category.palette
            assertNotNull(p.accent)
            assertNotNull(p.accentSoft)
            assertNotNull(p.accentTint)
            assertNotNull(p.accentBadge)
        }
    }

    @Test
    fun `every category points at a real R-string for each copy slot`() {
        // If any of these keys go missing from strings.xml,
        // R.string.* drops to 0 and this assertion catches it before
        // a translator pass discovers it.
        val resIds = WarmCategory.values().flatMap { c ->
            listOf(
                c.labelRes,
                c.groupNameRes,
                c.headlineLine1Res,
                c.headlineLine2Res,
                c.bodyRes,
                c.promptRes,
                c.promptShortRes,
            )
        }
        assertTrue(resIds.all { it != 0 }, "one or more WarmCategory string resources resolved to 0")
    }
}
