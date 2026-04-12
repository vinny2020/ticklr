package com.xaymaca.sit.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xaymaca.sit.R
import com.xaymaca.sit.SITApp

/**
 * Receives exact alarms fired by [TickleScheduler.scheduleNotification] and posts
 * the corresponding local notification to the user.
 *
 * Registered in AndroidManifest.xml with the com.xaymaca.sit.TICKLE_ALARM intent filter.
 * Also handles ACTION_BOOT_COMPLETED so scheduled alarms survive device restarts.
 */
class TickleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.xaymaca.sit.TICKLE_ALARM" -> handleTickleAlarm(context, intent)
            Intent.ACTION_BOOT_COMPLETED    -> handleBootCompleted(context)
        }
    }

    // -------------------------------------------------------------------------
    // Alarm: post the notification
    // -------------------------------------------------------------------------

    private fun handleTickleAlarm(context: Context, intent: Intent) {
        // Check POST_NOTIFICATIONS permission (required on Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val reminderId  = intent.getLongExtra("reminder_id", -1L)
        val contactName = intent.getStringExtra("contact_name")
            ?: context.getString(R.string.tickle_notification_contact_fallback)
        val note        = intent.getStringExtra("note") ?: ""

        if (reminderId == -1L) return

        val title = context.getString(R.string.tickle_notification_title, contactName)
        val body  = note.ifBlank { context.getString(R.string.tickle_notification_body) }

        val notification = NotificationCompat.Builder(context, SITApp.TICKLE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)
    }

    // -------------------------------------------------------------------------
    // Boot: reschedule any alarms that were lost when the device restarted
    // -------------------------------------------------------------------------

    private fun handleBootCompleted(context: Context) {
        // WorkManager persists its own schedule across reboots, so re-enqueue
        // the daily worker to ensure it is still registered after a cold boot.
        TickleScheduler.scheduleWorker(context)
    }
}
