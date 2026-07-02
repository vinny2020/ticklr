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
        // URI-encode each recipient before interpolating into the smsto: URI.
        // A raw '#' (or other URI-significant char) in a stored number would
        // otherwise be read as a fragment delimiter and truncate the recipient.
        // Encoding per-number preserves the ';' separator between recipients.
        val target = phoneNumbers.joinToString(";") { Uri.encode(it) }
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$target")
            putExtra("sms_body", message)
        }
    }
}
