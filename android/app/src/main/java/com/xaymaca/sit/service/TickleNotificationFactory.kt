package com.xaymaca.sit.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.xaymaca.sit.R
import com.xaymaca.sit.SITApp

/**
 * The shade actions offered on a tickle reminder notification (TIC-83). Each
 * carries the broadcast action string [TickleActionReceiver] filters on and a
 * request-code offset that keeps its [PendingIntent] distinct — see
 * [TickleNotificationFactory.actionRequestCode].
 */
enum class TickleNotificationAction(
    val intentAction: String,
    val requestCodeOffset: Int,
) {
    DONE("com.xaymaca.sit.TICKLE_DONE", 1),
    SNOOZE("com.xaymaca.sit.TICKLE_SNOOZE", 2),
}

/**
 * Single builder for the tickle reminder notification (TIC-83) so the WorkManager
 * sweep ([TickleWorker]) and the exact-alarm path ([TickleAlarmReceiver]) can't
 * drift — previously they duplicated the builder, and now both need the same
 * Done / Snooze actions.
 *
 * Tapping the body still routes through [TickleScheduler.contentPendingIntent]
 * (the TIC-82 deep link, unchanged). The two actions fire broadcasts at
 * [TickleActionReceiver], which validates against the DB and completes/snoozes.
 */
object TickleNotificationFactory {

    /**
     * Distinct, stable broadcast request code per (reminder, action). Two
     * PendingIntents that are `filterEquals` (same action string — extras are
     * ignored by the OS) but built with `FLAG_UPDATE_CURRENT` would clobber each
     * other's extras, so the same-action intents for different reminders MUST
     * get different request codes. `reminderId * SPAN + offset` guarantees
     * uniqueness across every (reminder, action) pair: with SPAN = 10 and
     * offsets 1/2, `10a+1 == 10b+2` is unsolvable for distinct reminders, and
     * `10a+1 == 10b+1` implies `a == b`. Matches the existing `reminderId.toInt()`
     * request-code convention used for the content and alarm PendingIntents.
     */
    fun actionRequestCode(reminderId: Long, action: TickleNotificationAction): Int =
        reminderId.toInt() * REQUEST_CODE_SPAN + action.requestCodeOffset

    /** The broadcast [Intent] delivered to [TickleActionReceiver] for [action]. */
    fun actionIntent(context: Context, reminderId: Long, action: TickleNotificationAction): Intent =
        Intent(action.intentAction).apply {
            // Package-restricted (not component-explicit) so the manifest
            // intent-filter resolves delivery while exported="false" blocks
            // third parties — mirrors TickleScheduler.buildNotificationIntent.
            setPackage(context.packageName)
            putExtra(TickleActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }

    private fun actionPendingIntent(
        context: Context,
        reminderId: Long,
        action: TickleNotificationAction,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        actionRequestCode(reminderId, action),
        actionIntent(context, reminderId, action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * Builds the reminder notification with Done / Snooze shade actions.
     *
     * [smallIcon] stays a parameter because the two call sites historically use
     * different icons (the worker a generic system icon, the alarm the launcher
     * mipmap); unifying that is out of TIC-83's scope, so each caller passes its
     * own and the observable notification is otherwise identical between paths.
     */
    fun buildReminderNotification(
        context: Context,
        reminderId: Long,
        contactId: Long?,
        title: String,
        body: String,
        @DrawableRes smallIcon: Int,
    ): Notification =
        NotificationCompat.Builder(context, SITApp.TICKLE_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(TickleScheduler.contentPendingIntent(context, reminderId, contactId))
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.tickle_row_action_done),
                actionPendingIntent(context, reminderId, TickleNotificationAction.DONE),
            )
            .addAction(
                0,
                context.getString(R.string.tickle_row_action_snooze),
                actionPendingIntent(context, reminderId, TickleNotificationAction.SNOOZE),
            )
            .build()

    private const val REQUEST_CODE_SPAN = 10
}
