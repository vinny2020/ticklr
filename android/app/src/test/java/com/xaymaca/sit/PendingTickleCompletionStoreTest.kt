package com.xaymaca.sit

import com.xaymaca.sit.service.PendingTickleCompletion
import com.xaymaca.sit.service.PendingTickleCompletionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TIC-82: semantics the NavGraph prompt effect relies on. The effect is keyed
 * on Unit and collects [PendingTickleCompletionStore.pending] directly — it
 * consumes the value before showing the snackbar, so the store must (a) emit
 * null on consume and (b) deliver that null to an already-running collector
 * sequentially, after the current emission's handler finishes, rather than
 * cancelling it (which was the bug with collectAsState + effect keying).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingTickleCompletionStoreTest {

    private val store = PendingTickleCompletionStore()

    @Test
    fun `store starts empty`() {
        assertNull(store.pending.value)
    }

    @Test
    fun `set stashes the completion`() {
        store.set(PendingTickleCompletion(reminderId = 7L, contactName = "Alice Smith"))

        assertEquals(7L, store.pending.value?.reminderId)
        assertEquals("Alice Smith", store.pending.value?.contactName)
    }

    @Test
    fun `consume emits null`() {
        store.set(PendingTickleCompletion(reminderId = 7L, contactName = "Alice Smith"))

        store.consume()

        assertNull(store.pending.value)
    }

    @Test
    fun `a second set replaces an un-shown pending completion`() {
        store.set(PendingTickleCompletion(reminderId = 7L, contactName = "Alice Smith"))
        store.set(PendingTickleCompletion(reminderId = 9L, contactName = "Bob Jones"))

        assertEquals(9L, store.pending.value?.reminderId)
    }

    @Test
    fun `consume inside a running collector does not cancel it - null arrives as the next emission`() = runTest {
        val seen = mutableListOf<PendingTickleCompletion?>()
        val collector = launch {
            store.pending.collect { pending ->
                seen += pending
                // Mirror the NavGraph effect: consume as soon as a value shows up.
                if (pending != null) store.consume()
            }
        }
        testScheduler.runCurrent() // initial null delivered

        store.set(PendingTickleCompletion(reminderId = 7L, contactName = "Alice Smith"))
        testScheduler.runCurrent()

        // The collector saw: initial null, the pending value, then the null from
        // its own consume() — delivered sequentially, not by cancellation.
        assertEquals(3, seen.size)
        assertNull(seen[0])
        assertEquals(7L, seen[1]?.reminderId)
        assertNull(seen[2])

        collector.cancelAndJoin()
    }
}
