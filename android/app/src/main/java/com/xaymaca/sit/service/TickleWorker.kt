package com.xaymaca.sit.service

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xaymaca.sit.R
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TickleWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val tickleRepository: TickleRepository,
    private val contactRepository: ContactRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val dueReminders = tickleRepository.getDueReminders(now)

        if (dueReminders.isEmpty()) return Result.success()

        // Check if POST_NOTIFICATIONS permission is granted (Android 13+)
        val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasNotificationPermission) return Result.success()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        dueReminders.forEach { reminder ->
            val contactName = reminder.contactId?.let { id ->
                contactRepository.getContactById(id)?.fullName
            } ?: context.getString(R.string.tickle_notification_contact_fallback)

            val title = context.getString(R.string.tickle_notification_title, contactName)
            val body = reminder.note.ifBlank { context.getString(R.string.tickle_notification_body) }

            val notification = NotificationCompat.Builder(context, SITApp.TICKLE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(reminder.id.toInt(), notification)
        }

        return Result.success()
    }
}
