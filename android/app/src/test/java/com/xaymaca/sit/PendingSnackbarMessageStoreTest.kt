package com.xaymaca.sit

import com.xaymaca.sit.service.PendingSnackbarMessageStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TIC-84: semantics the NavGraph save-confirmation snackbar effect relies on.
 * Mirrors [com.xaymaca.sit.PendingTickleCompletionStoreTest] — the effect is
 * keyed on Unit and collects [PendingSnackbarMessageStore.pending] directly,
 * consuming the value before showing the snackbar, so the store must (a) emit
 * null on consume and (b) deliver that null to an already-running collector
 * sequentially, after the current emission's handler finishes, rather than
 * cancelling it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingSnackbarMessageStoreTest {

    private val store = PendingSnackbarMessageStore()

    @Test
    fun `store starts empty`() {
        assertNull(store.pending.value)
    }

    @Test
    fun `set stashes the message`() {
        store.set("Tickle saved")

        assertEquals("Tickle saved", store.pending.value)
    }

    @Test
    fun `consume emits null`() {
        store.set("Tickle saved")

        store.consume()

        assertNull(store.pending.value)
    }

    @Test
    fun `a second set replaces an un-shown pending message`() {
        store.set("Tickle saved")
        store.set("Tickle updated")

        assertEquals("Tickle updated", store.pending.value)
    }

    @Test
    fun `consume inside a running collector does not cancel it - null arrives as the next emission`() = runTest {
        val seen = mutableListOf<String?>()
        val collector = launch {
            store.pending.collect { message ->
                seen += message
                // Mirror the NavGraph effect: consume as soon as a value shows up.
                if (message != null) store.consume()
            }
        }
        testScheduler.runCurrent() // initial null delivered

        store.set("Tickle saved")
        testScheduler.runCurrent()

        // The collector saw: initial null, the message, then the null from
        // its own consume() — delivered sequentially, not by cancellation.
        assertEquals(3, seen.size)
        assertNull(seen[0])
        assertEquals("Tickle saved", seen[1])
        assertNull(seen[2])

        collector.cancelAndJoin()
    }
}
