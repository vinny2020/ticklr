package com.xaymaca.sit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.service.TickleActionReceiver
import com.xaymaca.sit.service.TickleNotificationAction
import com.xaymaca.sit.service.TickleNotificationFactory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * TIC-83: the shade-action plumbing. The request-code scheme is pure and tested
 * directly; the broadcast Intent construction needs a Context for `packageName`,
 * so those cases run under Robolectric (JVM, no emulator).
 */
@RunWith(RobolectricTestRunner::class)
class TickleNotificationFactoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // -- action Intent construction ---------------------------------------------

    @Test
    fun `done action intent carries the DONE action string and reminder id`() {
        val intent = TickleNotificationFactory.actionIntent(
            context, reminderId = 42L, action = TickleNotificationAction.DONE
        )
        assertEquals("com.xaymaca.sit.TICKLE_DONE", intent.action)
        assertEquals(context.packageName, intent.`package`)
        assertEquals(42L, intent.getLongExtra(TickleActionReceiver.EXTRA_REMINDER_ID, -1L))
    }

    @Test
    fun `snooze action intent carries the SNOOZE action string and reminder id`() {
        val intent = TickleNotificationFactory.actionIntent(
            context, reminderId = 7L, action = TickleNotificationAction.SNOOZE
        )
        assertEquals("com.xaymaca.sit.TICKLE_SNOOZE", intent.action)
        assertEquals(7L, intent.getLongExtra(TickleActionReceiver.EXTRA_REMINDER_ID, -1L))
    }

    @Test
    fun `the receiver extra key matches the key the alarm path already reads`() {
        // TickleAlarmReceiver reads "reminder_id"; the action receiver must agree.
        assertEquals("reminder_id", TickleActionReceiver.EXTRA_REMINDER_ID)
    }

    // -- request-code uniqueness ------------------------------------------------

    @Test
    fun `the two actions of one reminder get distinct request codes`() {
        val done = TickleNotificationFactory.actionRequestCode(5L, TickleNotificationAction.DONE)
        val snooze = TickleNotificationFactory.actionRequestCode(5L, TickleNotificationAction.SNOOZE)
        assertNotEquals(done, snooze)
    }

    @Test
    fun `the same action for different reminders gets distinct request codes`() {
        val a = TickleNotificationFactory.actionRequestCode(1L, TickleNotificationAction.DONE)
        val b = TickleNotificationFactory.actionRequestCode(2L, TickleNotificationAction.DONE)
        assertNotEquals(a, b)
    }

    @Test
    fun `request codes are globally unique across a range of reminder-action pairs`() {
        val codes = mutableSetOf<Int>()
        for (id in 0L..500L) {
            for (action in TickleNotificationAction.entries) {
                val code = TickleNotificationFactory.actionRequestCode(id, action)
                assertTrue(
                    codes.add(code),
                    "Collision for reminder=$id action=$action code=$code"
                )
            }
        }
    }

    @Test
    fun `request code is stable for a given reminder and action`() {
        assertEquals(
            TickleNotificationFactory.actionRequestCode(99L, TickleNotificationAction.SNOOZE),
            TickleNotificationFactory.actionRequestCode(99L, TickleNotificationAction.SNOOZE)
        )
    }
}
