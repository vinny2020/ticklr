package com.xaymaca.sit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.data.db.SITDatabase
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContactTicklesTest {
    @Test
    fun scopedProfileFlowReflectsInsertsEditsSnoozesCompletionsReassignmentAndDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SITDatabase::class.java).build()
        val dao = db.tickleReminderDao()
        val updates = Channel<List<TickleReminder>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            dao.observeScheduledForContact(1).collect { updates.send(it) }
        }
        suspend fun awaitNotes(expected: List<String>) {
            withTimeout(10_000) {
                while (updates.receive().map { it.note } != expected) { }
            }
        }
        try {
            awaitNotes(emptyList())
            dao.insert(TickleReminder(contactId = 2, note = "Other person", nextDueDate = 50))
            dao.insert(TickleReminder(groupId = 1, note = "Group only", nextDueDate = 50))
            dao.insert(TickleReminder(contactId = 1, note = "History", status = TickleStatus.COMPLETED.name))
            val later = TickleReminder(contactId = 1, note = "Later", nextDueDate = 200)
            val laterId = dao.insert(later)
            val early = TickleReminder(contactId = 1, note = "Early", nextDueDate = 100)
            val earlyId = dao.insert(early)
            awaitNotes(listOf("Early", "Later"))
            dao.update(early.copy(id = earlyId, note = "Edited"))
            awaitNotes(listOf("Edited", "Later"))
            dao.update(early.copy(id = earlyId, note = "Snoozed", status = TickleStatus.SNOOZED.name, nextDueDate = 300))
            awaitNotes(listOf("Later", "Snoozed"))
            dao.update(early.copy(id = earlyId, status = TickleStatus.COMPLETED.name))
            awaitNotes(listOf("Later"))
            dao.update(later.copy(id = laterId, contactId = 2))
            awaitNotes(emptyList())
            val added = TickleReminder(contactId = 1, note = "New")
            val addedId = dao.insert(added)
            awaitNotes(listOf("New"))
            dao.delete(added.copy(id = addedId))
            awaitNotes(emptyList())
            assertEquals(emptyList<TickleReminder>(), dao.getByContactId(1).filter { it.status != TickleStatus.COMPLETED.name })
        } finally {
            observer.cancel()
            db.close()
        }
    }
}
