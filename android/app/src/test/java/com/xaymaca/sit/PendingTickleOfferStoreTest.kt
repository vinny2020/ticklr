package com.xaymaca.sit

import com.xaymaca.sit.service.PendingTickleOffer
import com.xaymaca.sit.service.PendingTickleOfferStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TIC-86: semantics the NavGraph offer effect relies on — identical to
 * [PendingTickleCompletionStoreTest]. The effect is keyed on Unit and collects
 * [PendingTickleOfferStore.pending] directly, consuming the value before showing
 * the snackbar, so the store must (a) emit null on consume and (b) deliver that
 * null to an already-running collector sequentially, not by cancelling it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingTickleOfferStoreTest {

    private val store = PendingTickleOfferStore()

    @Test
    fun `store starts empty`() {
        assertNull(store.pending.value)
    }

    @Test
    fun `set stashes the offer`() {
        store.set(PendingTickleOffer(contactId = 7L, contactName = "Alice Smith"))

        assertEquals(7L, store.pending.value?.contactId)
        assertEquals("Alice Smith", store.pending.value?.contactName)
    }

    @Test
    fun `consume emits null`() {
        store.set(PendingTickleOffer(contactId = 7L, contactName = "Alice Smith"))

        store.consume()

        assertNull(store.pending.value)
    }

    @Test
    fun `a second set replaces an un-shown offer`() {
        store.set(PendingTickleOffer(contactId = 7L, contactName = "Alice Smith"))
        store.set(PendingTickleOffer(contactId = 9L, contactName = "Bob Jones"))

        assertEquals(9L, store.pending.value?.contactId)
    }

    @Test
    fun `consume inside a running collector does not cancel it - null arrives as the next emission`() = runTest {
        val seen = mutableListOf<PendingTickleOffer?>()
        val collector = launch {
            store.pending.collect { offer ->
                seen += offer
                if (offer != null) store.consume()
            }
        }
        testScheduler.runCurrent() // initial null delivered

        store.set(PendingTickleOffer(contactId = 7L, contactName = "Alice Smith"))
        testScheduler.runCurrent()

        assertEquals(3, seen.size)
        assertNull(seen[0])
        assertEquals(7L, seen[1]?.contactId)
        assertNull(seen[2])

        collector.cancelAndJoin()
    }
}
