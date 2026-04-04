package com.xaymaca.sit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SITApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Disable ad personalization — Ticklr does not use advertising ID
        FirebaseAnalytics.getInstance(this).setUserProperty("allow_personalized_ads", "false")
        // Enable Crashlytics in debug so local crashes appear in Firebase Console
        // Set to false to suppress debug noise if needed
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
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
        const val KEY_SEND_SMS_DIRECTLY = "send_sms_directly"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        const val TICKLE_WORK_TAG = "tickle_daily_check"
    }
}
