package com.xaymaca.sit.ui.shared

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TIC-96: ComposeScreen's recipient dropdown and TickleEditScreen's contact
 * picker both silently truncated to 8 results with no indication more existed.
 * [PickerCap.cap] is the pure truncation + "is there more" decision behind the
 * "Showing N of Total" hint row.
 */
class PickerCapTest {

    @Test
    fun `a list at or under the limit is not truncated`() {
        val items = (1..8).toList()
        val capped = PickerCap.cap(items, limit = 8)
        assertEquals(8, capped.shown.size)
        assertEquals(8, capped.total)
        assertFalse(capped.isTruncated)
    }

    @Test
    fun `a list under the limit is shown in full`() {
        val items = (1..3).toList()
        val capped = PickerCap.cap(items, limit = 8)
        assertEquals(items, capped.shown)
        assertFalse(capped.isTruncated)
    }

    @Test
    fun `a list over the limit is truncated and reports the true total`() {
        val items = (1..20).toList()
        val capped = PickerCap.cap(items, limit = 8)
        assertEquals(8, capped.shown.size)
        assertEquals(20, capped.total)
        assertTrue(capped.isTruncated)
    }

    @Test
    fun `an empty list is never truncated`() {
        val capped = PickerCap.cap(emptyList<Int>(), limit = 8)
        assertTrue(capped.shown.isEmpty())
        assertFalse(capped.isTruncated)
    }

    @Test
    fun `default limit is 8`() {
        val items = (1..20).toList()
        val capped = PickerCap.cap(items)
        assertEquals(8, capped.shown.size)
    }
}
