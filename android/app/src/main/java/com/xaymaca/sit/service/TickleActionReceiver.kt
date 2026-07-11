package com.xaymaca.sit.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Handles the Done / Snooze action buttons on a tickle reminder notification
 * (TIC-83) — the cheapest response path, straight from the shade without opening
 * the app.
 *
 * Registered in AndroidManifest.xml, exported="false", filtering the two action
 * strings in [TickleNotificationAction]. The intents are package-restricted (set
 * in [TickleNotificationFactory.actionIntent]) but not component-explicit, so the
 * actions must stay in the intent-filter for the OS to resolve delivery, exactly
 * like [TickleAlarmReceiver].
 *
 * Both actions validate against the DB via [TickleScheduler.shouldPostFiredAlarm]
 * before acting: a reminder deleted or already COMPLETED (e.g. the user completed
 * it elsewhere, or the notification lingered) skips the mutation — but the
 * notification is dismissed either way, because the tap acknowledges it
 * regardless of whether the mutation applies. The mutation goes through the same
 * [TickleScheduler] transitions the in-app [com.xaymaca.sit.ui.tickle.TickleViewModel]
 * uses, so alarms re-sync identically. The process may be dead when this fires,
 * so — like [TickleAlarmReceiver] — DB work runs under goAsync() + a coroutine.
 */
@AndroidEntryPoint
class TickleActionReceiver : BroadcastReceiver() {

    @Inject lateinit var tickleRepository: TickleRepository
    @Inject lateinit var contactRepository: ContactRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = TickleNotificationAction.entries
            .firstOrNull { it.intentAction == intent.action } ?: return

        // -1L sentinel can't collide with a real id: TickleReminder.id is
        // @PrimaryKey(autoGenerate = true), so Room assigns rowids starting at 1.
        // (Same convention as TickleAlarmReceiver.)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // DB access is suspend; goAsync keeps the process alive until finish().
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAction(context, action, reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAction(
        context: Context,
        action: TickleNotificationAction,
        reminderId: Long,
    ) {
        // Mirror the fired-alarm validation (TIC-66): mutate only a reminder that
        // still exists and isn't completed. A stale action (reminder deleted or
        // already done since the notification posted) skips the mutation but MUST
        // still fall through to the cancel below.
        val reminder = tickleRepository.getReminderById(reminderId)
        if (reminder != null && TickleScheduler.shouldPostFiredAlarm(reminder)) {
            val updated = when (action) {
                TickleNotificationAction.DONE -> TickleScheduler.completedReminder(reminder)
                TickleNotificationAction.SNOOZE -> TickleScheduler.snoozedReminder(reminder)
            }
            tickleRepository.updateReminder(updated)
            // Keep the exact alarm in sync — arms the next occurrence / snooze-end
            // alarm or clears a stale one, exactly like the in-app path (TIC-66).
            TickleScheduler.syncAlarm(context, updated, alarmContactName(context, updated))
        }

        // Invariant: dismissal is UNCONDITIONAL. The tap acknowledges the
        // notification whether or not the mutation applied — a reminder completed
        // in-app after the notification posted must not leave a Done button that
        // visibly does nothing and a notification that never goes away.
        // (Cancels under the same id the builders posted with.)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
    }

    private suspend fun alarmContactName(
        context: Context,
        reminder: com.xaymaca.sit.data.model.TickleReminder,
    ): String =
        TickleScheduler.reminderDisplayName(
            contactName = reminder.contactId?.let { contactRepository.getContactById(it)?.fullName },
            groupName = reminder.groupId?.let { contactRepository.getGroupById(it)?.name },
            fallback = context.getString(R.string.tickle_notification_contact_fallback),
        )

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
