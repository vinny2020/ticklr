package com.xaymaca.sit.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.xaymaca.sit.R
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives exact alarms fired by [TickleScheduler.scheduleNotification] and posts
 * the corresponding local notification to the user.
 *
 * Registered in AndroidManifest.xml with the com.xaymaca.sit.TICKLE_ALARM intent filter.
 * Also handles ACTION_BOOT_COMPLETED: AlarmManager alarms do NOT survive a
 * restart, so every armable reminder's exact alarm is re-registered from the
 * database (TIC-66). The daily WorkManager sweep persists on its own but is
 * re-enqueued defensively.
 *
 * Both paths validate against the database rather than trusting intent extras:
 * a fired alarm posts nothing if its reminder was deleted, completed, or
 * rescheduled after the alarm was armed.
 */
@AndroidEntryPoint
class TickleAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var tickleRepository: TickleRepository
    @Inject lateinit var contactRepository: ContactRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != "com.xaymaca.sit.TICKLE_ALARM" && action != Intent.ACTION_BOOT_COMPLETED) return

        // DB access is suspend; goAsync keeps the process alive until finish().
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    "com.xaymaca.sit.TICKLE_ALARM" -> handleTickleAlarm(context, intent)
                    Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Alarm: validate against the DB, then post the notification
    // -------------------------------------------------------------------------

    private suspend fun handleTickleAlarm(context: Context, intent: Intent) {
        // Check POST_NOTIFICATIONS permission (required on Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val reminderId = intent.getLongExtra("reminder_id", -1L)
        if (reminderId == -1L) return

        // TIC-66: the armed alarm's extras are a snapshot from scheduling time.
        // Validate against current DB state — completed/deleted/rescheduled
        // reminders must not notify — and build the notification from the DB
        // so the name/note are current rather than stale extras.
        val reminder = tickleRepository.getReminderById(reminderId) ?: return
        if (!TickleScheduler.shouldPostFiredAlarm(reminder)) return

        val contactName = reminder.contactId?.let { cId ->
            contactRepository.getContactById(cId)?.fullName?.takeIf { it.isNotBlank() }
        } ?: context.getString(R.string.tickle_notification_contact_fallback)

        val title = context.getString(R.string.tickle_notification_title, contactName)
        val body = reminder.note.ifBlank { context.getString(R.string.tickle_notification_body) }

        // TIC-83: shared builder adds the Done / Snooze shade actions.
        val notification = TickleNotificationFactory.buildReminderNotification(
            context = context,
            reminderId = reminderId,
            contactId = reminder.contactId,
            title = title,
            body = body,
            smallIcon = R.mipmap.ic_launcher,
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)
    }

    // -------------------------------------------------------------------------
    // Boot: re-register every armable reminder's exact alarm from the DB
    // -------------------------------------------------------------------------

    private suspend fun handleBootCompleted(context: Context) {
        // WorkManager persists its own schedule across reboots; re-enqueue
        // defensively (KEEP policy makes this a no-op when already registered).
        TickleScheduler.scheduleWorker(context)

        // AlarmManager alarms are wiped by a restart — re-arm each future-due
        // reminder (TIC-66). Past-due ones are the daily worker's job.
        tickleRepository.getArmableReminders().forEach { reminder ->
            val contactName = reminder.contactId?.let { cId ->
                contactRepository.getContactById(cId)?.fullName?.takeIf { it.isNotBlank() }
            } ?: context.getString(R.string.tickle_notification_contact_fallback)
            TickleScheduler.syncAlarm(context, reminder, contactName)
        }
    }
}
