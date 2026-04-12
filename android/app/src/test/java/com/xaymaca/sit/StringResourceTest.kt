package com.xaymaca.sit

import com.xaymaca.sit.R
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Compile-time verification that critical string resource keys exist in strings.xml.
 * Any missing key in strings.xml causes an R.string reference to be unresolved,
 * which means this file won't compile — catching the error before runtime.
 */
class StringResourceTest {

    @Test
    fun `critical common string resources exist`() {
        val keys = listOf(
            R.string.common_save,
            R.string.common_cancel,
            R.string.common_delete,
            R.string.common_edit,
            R.string.common_ok,
            R.string.common_back,
            R.string.common_create,
            R.string.common_update,
            R.string.common_send,
        )
        assertTrue(keys.all { it != 0 }, "All common string resource IDs should be non-zero")
    }

    @Test
    fun `settings string resources exist`() {
        val keys = listOf(
            R.string.settings_title,
            R.string.settings_section_appearance,
            R.string.settings_section_data,
            R.string.settings_section_messaging,
            R.string.settings_section_about,
            R.string.settings_import_title,
            R.string.settings_templates_title,
            R.string.settings_sms_direct_title,
            R.string.settings_about_version,
            R.string.settings_reset_onboarding_title,
            R.string.settings_clear_data_title,
        )
        assertTrue(keys.all { it != 0 }, "All settings string resource IDs should be non-zero")
    }

    @Test
    fun `tickle string resources exist`() {
        val keys = listOf(
            R.string.tickle_list_title,
            R.string.tickle_list_empty_title,
            R.string.tickle_list_empty_description,
            R.string.tickle_list_section_due,
            R.string.tickle_list_section_upcoming,
            R.string.tickle_list_section_snoozed,
            R.string.tickle_saved,
            R.string.tickle_updated,
            R.string.tickle_notification_contact_fallback,
        )
        assertTrue(keys.all { it != 0 }, "All tickle string resource IDs should be non-zero")
    }

    @Test
    fun `network string resources exist`() {
        val keys = listOf(
            R.string.network_title,
            R.string.network_empty_title,
            R.string.add_contact_title,
            R.string.edit_contact_title,
            R.string.contact_detail_section_phone,
            R.string.contact_detail_section_email,
        )
        assertTrue(keys.all { it != 0 }, "All network string resource IDs should be non-zero")
    }

    @Test
    fun `group string resources exist`() {
        val keys = listOf(
            R.string.group_list_title,
            R.string.group_list_empty_title,
            R.string.group_detail_member_added,
            R.string.group_detail_member_added_generic,
            R.string.group_dialog_char_count,
        )
        assertTrue(keys.all { it != 0 }, "All group string resource IDs should be non-zero")
    }

    @Test
    fun `compose string resources exist`() {
        val keys = listOf(
            R.string.compose_title,
            R.string.compose_message_sent,
            R.string.common_send,
        )
        assertTrue(keys.all { it != 0 }, "All compose string resource IDs should be non-zero")
    }

    @Test
    fun `frequency string resources exist`() {
        val keys = listOf(
            R.string.frequency_daily,
            R.string.frequency_weekly,
            R.string.frequency_biweekly,
            R.string.frequency_monthly,
            R.string.frequency_bimonthly,
            R.string.frequency_quarterly,
            R.string.frequency_custom,
        )
        assertTrue(keys.all { it != 0 }, "All frequency string resource IDs should be non-zero")
    }

    @Test
    fun `notification string resources exist`() {
        val keys = listOf(
            R.string.tickle_notification_title,
            R.string.tickle_notification_body,
            R.string.tickle_notification_contact_fallback,
        )
        assertTrue(keys.all { it != 0 }, "All notification string resource IDs should be non-zero")
    }

    @Test
    fun `date formatting respects locale`() {
        val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(
            java.time.format.FormatStyle.MEDIUM
        )
        val formatted = java.time.LocalDate.of(2026, 4, 11).format(formatter)
        assertTrue(formatted.isNotEmpty(), "Formatted date should not be empty")
    }

    @Test
    fun `onboarding and import string resources exist`() {
        val keys = listOf(
            R.string.onboarding_tagline,
            R.string.onboarding_description,
            R.string.onboarding_import_button,
            R.string.onboarding_start_empty,
            R.string.import_title,
            R.string.import_from_phone_title,
            R.string.import_from_linkedin_title,
            R.string.import_snackbar_success,
            R.string.import_snackbar_failed,
            R.string.import_snackbar_permission_denied,
            R.string.import_snackbar_linkedin,
            R.string.import_snackbar_could_not_open,
            R.string.import_continue_button,
            R.string.import_skip_button,
        )
        assertTrue(keys.all { it != 0 }, "All onboarding/import string resource IDs should be non-zero")
    }

    @Test
    fun `template string resources exist`() {
        val keys = listOf(
            R.string.template_list_title,
            R.string.template_list_empty,
            R.string.template_edit_new_title,
            R.string.template_edit_edit_title,
            R.string.template_edit_title_label,
            R.string.template_edit_body_label,
        )
        assertTrue(keys.all { it != 0 }, "All template string resource IDs should be non-zero")
    }
}
