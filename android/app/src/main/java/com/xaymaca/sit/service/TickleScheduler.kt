package com.xaymaca.sit.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
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
     * - New tickle (`original == null`): one interval from `now`.
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
        now: Long = System.currentTimeMillis()
    ): Long {
        if (original == null) return nextDueDate(now, frequency, customDays)
        val scheduleChanged =
            original.frequency != frequency || original.customIntervalDays != customDays
        return if (scheduleChanged) nextDueDate(now, frequency, customDays) else original.nextDueDate
    }

    /**
     * Calculates the next due date from a base timestamp based on the given frequency.
     */
    fun nextDueDate(from: Long, frequency: String, customDays: Int? = null): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (frequency) {
            TickleFrequency.DAILY.name -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TickleFrequency.WEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 7)
            TickleFrequency.BIWEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 14)
            TickleFrequency.MONTHLY.name -> cal.add(Calendar.MONTH, 1)
            TickleFrequency.BIMONTHLY.name -> cal.add(Calendar.MONTH, 2)
            TickleFrequency.QUARTERLY.name -> cal.add(Calendar.MONTH, 3)
            TickleFrequency.CUSTOM.name -> cal.add(Calendar.DAY_OF_YEAR, customDays ?: 30)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Schedules a one-shot local notification for a specific tickle reminder.
     */
    fun scheduleNotification(
        context: Context,
        reminderId: Long,
        contactName: String,
        note: String,
        nextDueDate: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildNotificationIntent(context, reminderId, contactName, note)
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
        val intent = buildNotificationIntent(context, reminderId, "", "")
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
        contactName: String,
        note: String
    ): Intent {
        return Intent("com.xaymaca.sit.TICKLE_ALARM").apply {
            setPackage(context.packageName)
            putExtra("reminder_id", reminderId)
            putExtra("contact_name", contactName)
            putExtra("note", note)
        }
    }
}
