package com.xaymaca.sit

import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.service.TickleScheduler
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TickleSchedulerTest {

    private fun calAt(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calFrom(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    @Test
    fun `DAILY adds exactly one day`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.DAILY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `WEEKLY adds exactly seven days`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.WEEKLY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(22, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `BIWEEKLY adds exactly fourteen days`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.BIWEEKLY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(29, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `MONTHLY adds one calendar month`() {
        val from = calAt(2026, 1, 31)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.MONTHLY.name))
        // Calendar rolls Jan 31 + 1 month → Feb 28 (non-leap 2026)
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(2, result.get(Calendar.MONTH) + 1)
        assertEquals(28, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `BIMONTHLY adds two calendar months`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.BIMONTHLY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(5, result.get(Calendar.MONTH) + 1)
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `QUARTERLY adds three calendar months`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.QUARTERLY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(6, result.get(Calendar.MONTH) + 1)
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `CUSTOM uses provided interval days`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.CUSTOM.name, customDays = 10))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(25, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `CUSTOM defaults to 30 days when customDays is null`() {
        val from = calAt(2026, 3, 1)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.CUSTOM.name, customDays = null))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(31, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `unknown frequency defaults to one month`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, "BOGUS"))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(4, result.get(Calendar.MONTH) + 1)
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `result is always strictly after the input`() {
        val from = System.currentTimeMillis()
        for (freq in TickleFrequency.entries) {
            val next = TickleScheduler.nextDueDate(from, freq.name, customDays = 1)
            assertTrue(next > from, "Expected next > from for frequency ${freq.name}")
        }
    }

    @Test
    fun `QUARTERLY crosses year boundary correctly`() {
        val from = calAt(2025, 11, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.QUARTERLY.name))
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(2, result.get(Calendar.MONTH) + 1)
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    // -- nextDueDateForSave -----------------------------------------------------

    @Test
    fun `nextDueDateForSave on new tickle recomputes from now`() {
        val now = calAt(2026, 3, 15)
        val result = TickleScheduler.nextDueDateForSave(
            original = null,
            frequency = TickleFrequency.WEEKLY.name,
            customDays = null,
            now = now
        )
        assertEquals(now + TimeUnit.DAYS.toMillis(7), result)
    }

    @Test
    fun `nextDueDateForSave on note-only edit preserves existing nextDueDate`() {
        // The user changed only the note. We must NOT recompute — that would
        // wipe out any prior markComplete advancement and could fire the alarm
        // immediately if the original's `startDate + interval` is in the past.
        val now = calAt(2026, 3, 15)
        val futureDue = calAt(2026, 4, 30)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.MONTHLY.name,
            customIntervalDays = null,
            startDate = calAt(2025, 12, 1),
            nextDueDate = futureDue
        )
        val result = TickleScheduler.nextDueDateForSave(
            original = original,
            frequency = TickleFrequency.MONTHLY.name,
            customDays = null,
            now = now
        )
        assertEquals(futureDue, result)
    }

    @Test
    fun `nextDueDateForSave when frequency changed recomputes from now`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.WEEKLY.name,
            customIntervalDays = null,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        val result = TickleScheduler.nextDueDateForSave(
            original = original,
            frequency = TickleFrequency.MONTHLY.name,
            customDays = null,
            now = now
        )
        // Weekly → Monthly: should now be ~1 month from `now`, not from the old startDate
        val cal = calFrom(result)
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(4, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `nextDueDateForSave when custom days changed recomputes from now`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.CUSTOM.name,
            customIntervalDays = 14,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        val result = TickleScheduler.nextDueDateForSave(
            original = original,
            frequency = TickleFrequency.CUSTOM.name,
            customDays = 21,
            now = now
        )
        assertEquals(now + TimeUnit.DAYS.toMillis(21), result)
    }

    @Test
    fun `nextDueDateForSave when custom days went from null to a value recomputes`() {
        // Switching from MONTHLY (no custom days) to CUSTOM with 30 days is a
        // schedule change even though both come out to ~30 days — explicitness
        // matters because the user changed the frequency type.
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.CUSTOM.name,
            customIntervalDays = null,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        val result = TickleScheduler.nextDueDateForSave(
            original = original,
            frequency = TickleFrequency.CUSTOM.name,
            customDays = 30,
            now = now
        )
        assertEquals(now + TimeUnit.DAYS.toMillis(30), result)
    }
}
