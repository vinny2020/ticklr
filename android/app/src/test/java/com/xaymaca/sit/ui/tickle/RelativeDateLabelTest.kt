package com.xaymaca.sit.ui.tickle

import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

/**
 * Unit tests for `calendarDayDelta`, the day-difference helper used by
 * `relativeDateLabel` to render "Today" / "Tomorrow" / "In N days".
 *
 * The regression these tests guard: the previous millisecond-truncation
 * implementation labeled a tickle whose nextDue was less than 24 hours
 * away as "Today" — even when the actual due moment was on tomorrow's
 * calendar day. A daily tickle created at 1pm always tripped this because
 * its nextDue was exactly +24h, and by the time the list rendered the
 * raw diff was a few ms under 86_400_000.
 */
class RelativeDateLabelTest {

    private val utc = ZoneId.of("UTC")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `delta is zero when target equals now`() {
        val now = millis(2026, 5, 1, 13, 0)
        assertEquals(0, calendarDayDelta(now, now, utc))
    }

    @Test
    fun `delta is one when target is tomorrow same time`() {
        val now = millis(2026, 5, 1, 13, 0)
        val target = millis(2026, 5, 2, 13, 0)
        assertEquals(1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta is one when target is tomorrow earlier in the day`() {
        // The regression case: a daily tickle created at 1pm has nextDue at
        // exactly +24h. Milliseconds later when the list renders, the raw
        // diff is ~23h59m999ms → integer-divided to 0 → labeled "Today".
        // Calendar-day comparison gives 1 → labeled "Tomorrow".
        val now = millis(2026, 5, 1, 13, 0)
        val target = millis(2026, 5, 2, 12, 59) // less than 24h later, but tomorrow
        assertEquals(1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta is one when target crosses midnight by minutes`() {
        val now = millis(2026, 5, 1, 23, 59)
        val target = millis(2026, 5, 2, 0, 1)
        assertEquals(1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta is zero when target is same calendar day many hours away`() {
        val now = millis(2026, 5, 1, 9, 0)
        val target = millis(2026, 5, 1, 22, 0)
        assertEquals(0, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta is negative one when target is yesterday`() {
        val now = millis(2026, 5, 2, 13, 0)
        val target = millis(2026, 5, 1, 13, 0)
        assertEquals(-1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta is negative one when target was just before midnight yesterday`() {
        // Mirror of the daily-tickle regression case in the past direction.
        val now = millis(2026, 5, 2, 0, 1)
        val target = millis(2026, 5, 1, 23, 59)
        assertEquals(-1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta crosses year boundary correctly`() {
        val now = millis(2025, 12, 31, 12, 0)
        val target = millis(2026, 1, 1, 1, 0)
        assertEquals(1, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta handles multi-day gaps`() {
        val now = millis(2026, 5, 1, 13, 0)
        val target = millis(2026, 5, 8, 13, 0)
        assertEquals(7, calendarDayDelta(now, target, utc))
    }

    @Test
    fun `delta handles weekly tickle near time boundary`() {
        // Weekly tickle: nextDue = now + 7 days. By the time the list renders,
        // raw diff is ~6d 23h 59m 999ms. Old code would label "In 6 days".
        // Calendar-day comparison correctly gives 7 → "In 7 days".
        val now = millis(2026, 5, 1, 13, 0)
        val target = millis(2026, 5, 8, 12, 59)
        assertEquals(7, calendarDayDelta(now, target, utc))
    }
}
