package com.xaymaca.sit.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsService @Inject constructor() {

    /**
     * Creates an Intent to open the user's default SMS app pre-filled with recipients
     * and message body. The user reviews and sends the message themselves — Ticklr
     * never sends SMS directly. This complies with Google Play's SMS/Call Log policy
     * (only default SMS handlers may use SEND_SMS) and matches iOS behavior, where
     * silent SMS sending is also prohibited.
     *
     * For a single recipient, uses the smsto: URI. For multiple, joins numbers with
     * semicolons (group MMS behavior varies by SMS app).
     */
    fun sendSmsIntent(context: Context, phoneNumbers: List<String>, message: String): Intent {
        val target = if (phoneNumbers.size == 1) phoneNumbers.first() else phoneNumbers.joinToString(";")
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$target")
            putExtra("sms_body", message)
        }
    }
}
