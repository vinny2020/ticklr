package com.xaymaca.sit.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import androidx.work.*
import com.xaymaca.sit.MainActivity
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.ui.nav.Screen
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TickleScheduler {

    /**
     * Schedules the daily WorkManager check for due tickle reminders.
     * Fires once per day with an initial delay to the next 9am.
     */
    fun scheduleWorker(context: Context) {
        val initialDelay = millisUntilNextNineAm()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<TickleWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(SITApp.TICKLE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SITApp.TICKLE_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels the daily tickle check worker.
     */
    fun cancelWorker(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SITApp.TICKLE_WORK_TAG)
    }

    /**
     * Decides the `nextDueDate` to persist when saving a tickle.
     *
     * - New one-time / annual tickle: use the selected date semantics.
     * - New interval tickle (`original == null`): one interval from `now`.
     * - Edit where frequency or custom interval changed: one interval from `now`.
     *   "Next" should mean "one interval from now," not from the original
     *   `startDate` (which is in the past).
     * - Pure note/contact edit: preserves the existing `nextDueDate` so prior
     *   `markComplete` advancement isn't wiped out by a typo fix.
     */
    fun nextDueDateForSave(
        original: TickleReminder?,
        frequency: String,
        customDays: Int?,
        now: Long = System.currentTimeMillis(),
        startDate: Long? = null
    ): Long {
        val effectiveStartDate = startDate ?: original?.startDate ?: now
        if (original == null) return initialNextDueDate(effectiveStartDate, frequency, customDays, now)
        return if (scheduleChanged(original, frequency, customDays, effectiveStartDate)) {
            initialNextDueDate(effectiveStartDate, frequency, customDays, now)
        } else {
            original.nextDueDate
        }
    }

    /**
     * Whether an edit touched a schedule field (frequency, custom interval, or
     * start date). Callers use this to decide whether to recompute schedule-derived
     * state (`nextDueDate`, `status`) or preserve the original's — so a note/contact
     * edit doesn't wipe completion history or un-snooze a reminder (TIC-67).
     */
    fun scheduleChanged(
        original: TickleReminder,
        frequency: String,
        customDays: Int?,
        startDate: Long
    ): Boolean =
        original.frequency != frequency ||
            original.customIntervalDays != customDays ||
            original.startDate != startDate

    /**
     * Calculates the next due date from a base timestamp based on the given frequency.
     */
    fun nextDueDate(from: Long, frequency: String, customDays: Int? = null): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (frequency) {
            TickleFrequency.ONE_TIME.name -> return from
            TickleFrequency.DAILY.name -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TickleFrequency.WEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 7)
            TickleFrequency.BIWEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 14)
            TickleFrequency.MONTHLY.name -> cal.add(Calendar.MONTH, 1)
            TickleFrequency.BIMONTHLY.name -> cal.add(Calendar.MONTH, 2)
            TickleFrequency.QUARTERLY.name -> cal.add(Calendar.MONTH, 3)
            TickleFrequency.ANNUAL.name -> cal.add(Calendar.YEAR, 1)
            TickleFrequency.CUSTOM.name -> cal.add(Calendar.DAY_OF_YEAR, customDays ?: 30)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun initialNextDueDate(
        startDate: Long,
        frequency: String,
        customDays: Int? = null,
        now: Long = System.currentTimeMillis()
    ): Long {
        return when (frequency) {
            TickleFrequency.ONE_TIME.name -> startDate
            TickleFrequency.ANNUAL.name -> nextAnnualDate(after = now, matchingMonthDayOf = startDate)
            else -> nextDueDate(now, frequency, customDays)
        }
    }

    /**
     * Due-ness is date-based (TIC-61, matching iOS): a reminder is due once its
     * nextDueDate has passed, whether ACTIVE or SNOOZED — snoozing only pushes
     * the date out, it is not a mute. COMPLETED reminders are never due.
     */
    fun isDue(reminder: TickleReminder, now: Long): Boolean =
        reminder.nextDueDate <= now &&
            (reminder.status == TickleStatus.ACTIVE.name || reminder.status == TickleStatus.SNOOZED.name)

    /** ACTIVE with a future due date — the Upcoming section. */
    fun isUpcoming(reminder: TickleReminder, now: Long): Boolean =
        reminder.status == TickleStatus.ACTIVE.name && reminder.nextDueDate > now

    /** SNOOZED and still inside its snooze window — the Snoozed section. */
    fun isSnoozedWaiting(reminder: TickleReminder, now: Long): Boolean =
        reminder.status == TickleStatus.SNOOZED.name && reminder.nextDueDate > now

    /**
     * The next due date after completing a recurring reminder. Annual reminders
     * anchor on [startDate], never on the reminder's current nextDueDate —
     * snooze() overwrites nextDueDate, so deriving the next occurrence from it
     * would shift the anniversary by the snooze amount, permanently (TIC-62).
     */
    fun nextDueDateOnComplete(
        frequency: String,
        startDate: Long,
        customDays: Int? = null,
        now: Long = System.currentTimeMillis()
    ): Long {
        return if (frequency == TickleFrequency.ANNUAL.name) {
            nextAnnualDate(after = now, matchingMonthDayOf = startDate)
        } else {
            nextDueDate(from = now, frequency = frequency, customDays = customDays)
        }
    }

    fun nextAnnualDate(after: Long, matchingMonthDayOf: Long): Long {
        val source = Calendar.getInstance().apply { timeInMillis = matchingMonthDayOf }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.MONTH, source.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, source.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, source.get(Calendar.MINUTE))
            set(Calendar.SECOND, source.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, source.get(Calendar.MILLISECOND))
        }
        if (candidate.timeInMillis <= after) {
            candidate.add(Calendar.YEAR, 1)
        }
        return candidate.timeInMillis
    }

    /**
     * Whether a reminder should have an exact alarm armed: it will become due
     * in the future (ACTIVE, or SNOOZED until its snooze-end date) and isn't
     * completed. Past-due reminders are the Due UI's / daily worker's job —
     * arming an alarm for a past date would fire immediately.
     */
    fun shouldArmAlarm(reminder: TickleReminder, now: Long = System.currentTimeMillis()): Boolean =
        reminder.nextDueDate > now &&
            (reminder.status == TickleStatus.ACTIVE.name || reminder.status == TickleStatus.SNOOZED.name)

    /**
     * Whether a fired alarm should still post its notification, validated
     * against current DB state (TIC-66): the reminder must still exist, not be
     * completed, and actually be due — if the user completed or snoozed it
     * after the alarm was armed, its nextDueDate moved into the future and the
     * firing is stale.
     */
    fun shouldPostFiredAlarm(reminder: TickleReminder?, now: Long = System.currentTimeMillis()): Boolean =
        reminder != null &&
            reminder.status != TickleStatus.COMPLETED.name &&
            reminder.nextDueDate <= now

    /**
     * The reminder as it should be persisted after the user marks it done
     * (TIC-83). Pure so the in-app `TickleViewModel.markComplete` and the
     * notification-shade `TickleActionReceiver` produce identical state — a
     * one-time tickle becomes COMPLETED; a recurring one stays ACTIVE and
     * advances to its next occurrence (annual anchors on startDate, TIC-62).
     * Callers persist the result and then call [syncAlarm].
     */
    fun completedReminder(
        reminder: TickleReminder,
        now: Long = System.currentTimeMillis()
    ): TickleReminder =
        if (reminder.frequency == TickleFrequency.ONE_TIME.name) {
            reminder.copy(
                lastCompletedDate = now,
                status = TickleStatus.COMPLETED.name
            )
        } else {
            reminder.copy(
                lastCompletedDate = now,
                nextDueDate = nextDueDateOnComplete(
                    frequency = reminder.frequency,
                    startDate = reminder.startDate,
                    customDays = reminder.customIntervalDays,
                    now = now
                ),
                status = TickleStatus.ACTIVE.name
            )
        }

    /**
     * The reminder as it should be persisted after the user snoozes it (TIC-83).
     * Pure so the in-app path and the notification-shade `TickleActionReceiver`
     * share one definition of "snooze": push `nextDueDate` out by [days] and
     * mark SNOOZED (a date shift, not a mute — TIC-61). Callers persist the
     * result and then call [syncAlarm].
     */
    fun snoozedReminder(
        reminder: TickleReminder,
        days: Int = 7,
        now: Long = System.currentTimeMillis()
    ): TickleReminder =
        reminder.copy(
            nextDueDate = now + days * 24L * 60 * 60 * 1000,
            status = TickleStatus.SNOOZED.name
        )

    /**
     * Single source of truth for a reminder's exact alarm (TIC-66). Call after
     * EVERY reminder mutation: arms the alarm when [shouldArmAlarm], otherwise
     * cancels any stale one. Scheduling reuses the reminder-id request code, so
     * arming also replaces a previously armed alarm for the same reminder.
     */
    fun syncAlarm(context: Context, reminder: TickleReminder, contactName: String) {
        if (shouldArmAlarm(reminder)) {
            scheduleNotification(
                context, reminder.id, reminder.contactId, contactName, reminder.note, reminder.nextDueDate
            )
        } else {
            cancelNotification(context, reminder.id)
        }
    }

    /**
     * Schedules a one-shot local notification for a specific tickle reminder.
     */
    fun scheduleNotification(
        context: Context,
        reminderId: Long,
        contactId: Long?,
        contactName: String,
        note: String,
        nextDueDate: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildNotificationIntent(context, reminderId, contactId, contactName, note)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextDueDate,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted — use inexact alarm instead
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextDueDate, pendingIntent)
        }
    }

    /**
     * Cancels a previously scheduled notification for a reminder.
     */
    fun cancelNotification(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildNotificationIntent(context, reminderId, null, "", "")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }

        // Also dismiss any active notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
    }

    // Milliseconds from now until the next 9:00 AM
    private fun millisUntilNextNineAm(): Long {
        val now = Calendar.getInstance()
        val nineAm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (nineAm.before(now)) {
            nineAm.add(Calendar.DAY_OF_YEAR, 1)
        }
        return nineAm.timeInMillis - now.timeInMillis
    }

    private fun buildNotificationIntent(
        context: Context,
        reminderId: Long,
        contactId: Long?,
        contactName: String,
        note: String
    ): Intent {
        return Intent("com.xaymaca.sit.TICKLE_ALARM").apply {
            setPackage(context.packageName)
            putExtra("reminder_id", reminderId)
            putExtra("contact_id", contactId ?: -1L)
            putExtra("contact_name", contactName)
            putExtra("note", note)
        }
    }

    /**
     * Builds the tap action for a tickle notification (TIC-35): a deep link into
     * Compose, pre-addressed to the contact when one is attached. Used as the
     * notification's content intent on both the alarm and WorkManager paths.
     * The synthetic back stack returns to the Tickle start destination.
     */
    fun contentPendingIntent(context: Context, reminderId: Long, contactId: Long?): PendingIntent {
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            // TIC-82: carry the reminderId so returning from the SMS handoff can
            // prompt "mark [name]'s tickle done?".
            Uri.parse(Screen.Compose.deepLinkUri(contactId, reminderId)),
            context,
            MainActivity::class.java
        )
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLink)
            getPendingIntent(
                reminderId.toInt(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }
}
