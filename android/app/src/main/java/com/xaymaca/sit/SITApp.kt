package com.xaymaca.sit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import com.xaymaca.sit.data.repository.MessageTemplateSeed
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SITApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var messageTemplateRepository: MessageTemplateRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Enable Crashlytics in debug so local crashes appear in Firebase Console
        // Set to false to suppress debug noise if needed
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        seedDefaultTemplate()
    }

    private fun seedDefaultTemplate() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appScope.launch {
            MessageTemplateSeed.seedDefaultIfNeeded(messageTemplateRepository, prefs)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TICKLE_CHANNEL_ID,
            "Tickle Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ticklr reminders"
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val TICKLE_CHANNEL_ID = "tickle_channel"
        const val PREFS_NAME = "sit_prefs"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        const val TICKLE_WORK_TAG = "tickle_daily_check"
    }
}
