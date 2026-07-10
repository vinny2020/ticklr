package com.xaymaca.sit

import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
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
    fun `ONE_TIME returns selected date unchanged`() {
        val from = calAt(2026, 3, 15)
        val result = TickleScheduler.nextDueDate(from, TickleFrequency.ONE_TIME.name)
        assertEquals(from, result)
    }

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
    fun `ANNUAL adds one calendar year`() {
        val from = calAt(2026, 3, 15)
        val result = calFrom(TickleScheduler.nextDueDate(from, TickleFrequency.ANNUAL.name))
        assertEquals(2027, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
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
        for (freq in TickleFrequency.entries.filter { it != TickleFrequency.ONE_TIME }) {
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
    fun `nextDueDateForSave on new one-time tickle uses selected date`() {
        val now = calAt(2026, 3, 15)
        val selected = calAt(2026, 3, 22)
        val result = TickleScheduler.nextDueDateForSave(
            original = null,
            frequency = TickleFrequency.ONE_TIME.name,
            customDays = null,
            now = now,
            startDate = selected
        )
        assertEquals(selected, result)
    }

    @Test
    fun `nextDueDateForSave on new annual tickle uses next matching month day`() {
        val now = calAt(2026, 6, 2)
        val selected = calAt(2025, 6, 1)
        val result = calFrom(
            TickleScheduler.nextDueDateForSave(
                original = null,
                frequency = TickleFrequency.ANNUAL.name,
                customDays = null,
                now = now,
                startDate = selected
            )
        )
        assertEquals(2027, result.get(Calendar.YEAR))
        assertEquals(6, result.get(Calendar.MONTH) + 1)
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH))
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

    // -- scheduleChanged (preserve-vs-recompute on edit, TIC-67) ----------------

    @Test
    fun `scheduleChanged is false for a note or contact-only edit`() {
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.MONTHLY.name,
            customIntervalDays = null,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        // Same schedule fields as the original — only note/contact differ.
        assertTrue(
            !TickleScheduler.scheduleChanged(
                original = original,
                frequency = TickleFrequency.MONTHLY.name,
                customDays = null,
                startDate = original.startDate
            )
        )
    }

    @Test
    fun `scheduleChanged is true when frequency changes`() {
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.WEEKLY.name,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        assertTrue(
            TickleScheduler.scheduleChanged(
                original = original,
                frequency = TickleFrequency.MONTHLY.name,
                customDays = null,
                startDate = original.startDate
            )
        )
    }

    @Test
    fun `scheduleChanged is true when custom interval changes`() {
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.CUSTOM.name,
            customIntervalDays = 14,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 4, 30)
        )
        assertTrue(
            TickleScheduler.scheduleChanged(
                original = original,
                frequency = TickleFrequency.CUSTOM.name,
                customDays = 21,
                startDate = original.startDate
            )
        )
    }

    @Test
    fun `scheduleChanged is true when start date changes`() {
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.ANNUAL.name,
            startDate = calAt(2025, 12, 1),
            nextDueDate = calAt(2026, 12, 1)
        )
        assertTrue(
            TickleScheduler.scheduleChanged(
                original = original,
                frequency = TickleFrequency.ANNUAL.name,
                customDays = null,
                startDate = calAt(2025, 12, 25)
            )
        )
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

    // -- nextDueDateOnComplete (TIC-62) -----------------------------------------

    @Test
    fun `completing an annual tickle anchors on startDate not nextDueDate`() {
        // The snooze-corruption scenario: birthday tickle anchored Mar 10 fires,
        // user snoozes (nextDueDate becomes Mar 17), then completes a week later.
        // The next occurrence must be Mar 10 of next year — the startDate anchor —
        // not the snooze-shifted Mar 17.
        val startDate = calAt(2025, 3, 10)
        val completedAt = calAt(2026, 3, 17)
        val result = calFrom(
            TickleScheduler.nextDueDateOnComplete(
                frequency = TickleFrequency.ANNUAL.name,
                startDate = startDate,
                now = completedAt
            )
        )
        assertEquals(2027, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(10, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `completing an annual tickle before its anniversary stays in the same year`() {
        val startDate = calAt(2025, 6, 20)
        val completedAt = calAt(2026, 2, 1)
        val result = calFrom(
            TickleScheduler.nextDueDateOnComplete(
                frequency = TickleFrequency.ANNUAL.name,
                startDate = startDate,
                now = completedAt
            )
        )
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(6, result.get(Calendar.MONTH) + 1)
        assertEquals(20, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `completing a non-annual tickle advances one interval from now`() {
        val startDate = calAt(2025, 12, 1)
        val completedAt = calAt(2026, 3, 15)
        val result = TickleScheduler.nextDueDateOnComplete(
            frequency = TickleFrequency.WEEKLY.name,
            startDate = startDate,
            now = completedAt
        )
        assertEquals(completedAt + TimeUnit.DAYS.toMillis(7), result)
    }

    // -- Section semantics: isDue / isUpcoming / isSnoozedWaiting (TIC-61) ------

    private fun reminder(status: TickleStatus, dueAt: Long) = TickleReminder(
        id = 1L,
        frequency = TickleFrequency.WEEKLY.name,
        startDate = calAt(2026, 1, 1),
        nextDueDate = dueAt,
        status = status.name
    )

    @Test
    fun `snoozed reminder becomes due when its snooze window elapses`() {
        // The TIC-61 regression: snooze used to be a permanent mute because
        // every due filter required status == ACTIVE.
        val now = calAt(2026, 3, 15)
        val snoozeEnded = reminder(TickleStatus.SNOOZED, dueAt = calAt(2026, 3, 14))
        assertTrue(TickleScheduler.isDue(snoozeEnded, now))
        assertTrue(!TickleScheduler.isSnoozedWaiting(snoozeEnded, now))
    }

    @Test
    fun `snoozed reminder inside its window is waiting not due`() {
        val now = calAt(2026, 3, 15)
        val stillSnoozing = reminder(TickleStatus.SNOOZED, dueAt = calAt(2026, 3, 20))
        assertTrue(!TickleScheduler.isDue(stillSnoozing, now))
        assertTrue(TickleScheduler.isSnoozedWaiting(stillSnoozing, now))
        assertTrue(!TickleScheduler.isUpcoming(stillSnoozing, now))
    }

    @Test
    fun `active reminder sections split on the due date`() {
        val now = calAt(2026, 3, 15)
        val due = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 3, 10))
        val upcoming = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 3, 20))
        assertTrue(TickleScheduler.isDue(due, now))
        assertTrue(!TickleScheduler.isUpcoming(due, now))
        assertTrue(TickleScheduler.isUpcoming(upcoming, now))
        assertTrue(!TickleScheduler.isDue(upcoming, now))
    }

    @Test
    fun `completed reminder is never due upcoming or snoozed`() {
        val now = calAt(2026, 3, 15)
        val done = reminder(TickleStatus.COMPLETED, dueAt = calAt(2026, 3, 10))
        assertTrue(!TickleScheduler.isDue(done, now))
        assertTrue(!TickleScheduler.isUpcoming(done, now))
        assertTrue(!TickleScheduler.isSnoozedWaiting(done, now))
    }

    // -- Alarm lifecycle: shouldArmAlarm / shouldPostFiredAlarm (TIC-66) --------

    @Test
    fun `alarm is armed for future-due active and snoozed reminders only`() {
        val now = calAt(2026, 3, 15)
        val futureActive = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 3, 20))
        val futureSnoozed = reminder(TickleStatus.SNOOZED, dueAt = calAt(2026, 3, 22))
        val pastActive = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 3, 10))
        val futureCompleted = reminder(TickleStatus.COMPLETED, dueAt = calAt(2026, 3, 20))

        assertTrue(TickleScheduler.shouldArmAlarm(futureActive, now))
        // Snoozed = alarm at snooze end, not silence — snooze is not a mute.
        assertTrue(TickleScheduler.shouldArmAlarm(futureSnoozed, now))
        // Past-due belongs to the Due UI / daily worker; arming would fire immediately.
        assertTrue(!TickleScheduler.shouldArmAlarm(pastActive, now))
        assertTrue(!TickleScheduler.shouldArmAlarm(futureCompleted, now))
    }

    @Test
    fun `fired alarm posts only when the reminder is still genuinely due`() {
        val now = calAt(2026, 3, 15)
        val due = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 3, 15))
        val completed = reminder(TickleStatus.COMPLETED, dueAt = calAt(2026, 3, 15))
        // Completed or snoozed AFTER the alarm was armed: nextDueDate moved to
        // the future, so this firing is stale and must stay silent.
        val rescheduled = reminder(TickleStatus.ACTIVE, dueAt = calAt(2026, 4, 15))
        val snoozedPastEnd = reminder(TickleStatus.SNOOZED, dueAt = calAt(2026, 3, 14))

        assertTrue(TickleScheduler.shouldPostFiredAlarm(due, now))
        assertTrue(TickleScheduler.shouldPostFiredAlarm(snoozedPastEnd, now))
        assertTrue(!TickleScheduler.shouldPostFiredAlarm(completed, now))
        assertTrue(!TickleScheduler.shouldPostFiredAlarm(rescheduled, now))
        assertTrue(!TickleScheduler.shouldPostFiredAlarm(null, now))
    }

    // -- completedReminder / snoozedReminder transitions (TIC-83) ---------------
    // Shared by the in-app TickleViewModel and the notification-shade
    // TickleActionReceiver, so both produce identical persisted state.

    @Test
    fun `completing a one-time reminder marks it COMPLETED and stamps completion`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.ONE_TIME.name,
            startDate = calAt(2026, 3, 10),
            nextDueDate = calAt(2026, 3, 10),
            status = TickleStatus.ACTIVE.name
        )
        val result = TickleScheduler.completedReminder(original, now)
        assertEquals(TickleStatus.COMPLETED.name, result.status)
        assertEquals(now, result.lastCompletedDate)
        // One-time completion doesn't advance the due date.
        assertEquals(original.nextDueDate, result.nextDueDate)
    }

    @Test
    fun `completing a recurring reminder stays ACTIVE and advances one interval`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.WEEKLY.name,
            startDate = calAt(2026, 1, 1),
            nextDueDate = calAt(2026, 3, 10),
            status = TickleStatus.ACTIVE.name
        )
        val result = TickleScheduler.completedReminder(original, now)
        assertEquals(TickleStatus.ACTIVE.name, result.status)
        assertEquals(now, result.lastCompletedDate)
        assertEquals(now + TimeUnit.DAYS.toMillis(7), result.nextDueDate)
    }

    @Test
    fun `completing a snoozed annual reminder anchors next occurrence on startDate`() {
        // Same TIC-62 anti-corruption path the action button must honour: a
        // snoozed annual (nextDueDate shifted) completes to the startDate anchor.
        val now = calAt(2026, 3, 17)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.ANNUAL.name,
            startDate = calAt(2025, 3, 10),
            nextDueDate = calAt(2026, 3, 17),
            status = TickleStatus.SNOOZED.name
        )
        val result = calFrom(TickleScheduler.completedReminder(original, now).nextDueDate)
        assertEquals(2027, result.get(Calendar.YEAR))
        assertEquals(3, result.get(Calendar.MONTH) + 1)
        assertEquals(10, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `snoozing sets SNOOZED and pushes the due date out by the default seven days`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.MONTHLY.name,
            startDate = calAt(2026, 1, 1),
            nextDueDate = calAt(2026, 3, 14),
            status = TickleStatus.ACTIVE.name
        )
        val result = TickleScheduler.snoozedReminder(original, now = now)
        assertEquals(TickleStatus.SNOOZED.name, result.status)
        assertEquals(now + TimeUnit.DAYS.toMillis(7), result.nextDueDate)
    }

    @Test
    fun `snoozing honours a custom day count`() {
        val now = calAt(2026, 3, 15)
        val original = TickleReminder(
            id = 1L,
            frequency = TickleFrequency.MONTHLY.name,
            startDate = calAt(2026, 1, 1),
            nextDueDate = calAt(2026, 3, 14),
            status = TickleStatus.ACTIVE.name
        )
        val result = TickleScheduler.snoozedReminder(original, days = 3, now = now)
        assertEquals(now + TimeUnit.DAYS.toMillis(3), result.nextDueDate)
    }
}
