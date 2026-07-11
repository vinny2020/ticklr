package com.xaymaca.sit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.data.db.SITDatabase
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TIC-87 — DB-backed restore mechanics for the low-stakes swipe deletes that now
 * offer UNDO instead of nothing. Runs on a real in-memory Room database
 * (Robolectric/JVM, no emulator) so the id-preservation guarantee for template
 * re-insert and the membership re-add are exercised against actual Room behaviour,
 * not a fake.
 */
@RunWith(RobolectricTestRunner::class)
class UndoRestoreTest {

    private lateinit var db: SITDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SITDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -- Template swipe-delete → UNDO re-insert (id preserved) -------------------

    @Test
    fun `restoring a deleted template re-inserts it with its exact prior id`() = runBlocking {
        val repo = MessageTemplateRepository(db.messageTemplateDao())

        // Insert, then read back the row Room assigned so we hold a full snapshot.
        val newId = repo.insertTemplate(MessageTemplate(title = "Checking in", body = "Hey!"))
        val snapshot = repo.getTemplateById(newId)
        assertNotNull(snapshot)
        assertTrue(snapshot!!.id != 0L)

        // Swipe-delete removes it.
        repo.deleteTemplate(snapshot)
        assertEquals(0, repo.count())

        // UNDO restores the exact snapshot — non-zero id means Room keeps it
        // (autoGenerate only mints an id when id == 0; insert is onConflict=REPLACE).
        repo.insertTemplate(snapshot)

        val restored = repo.getTemplateById(newId)
        assertNotNull(restored)
        assertEquals(snapshot.id, restored!!.id)
        assertEquals("Checking in", restored.title)
        assertEquals("Hey!", restored.body)
        assertEquals(snapshot.createdAt, restored.createdAt)
        assertEquals(1, repo.count())
    }

    // -- Group member swipe-remove → UNDO re-add membership ----------------------

    @Test
    fun `re-adding a removed member restores the membership row`() = runBlocking {
        val repo = ContactRepository(
            db.contactDao(),
            db.contactGroupDao(),
            db.tickleReminderDao(),
        )
        val contactId = 11L
        val groupId = 5L

        repo.addContactToGroup(contactId, groupId)
        assertEquals(1, db.contactGroupDao().getMemberCount(groupId).first())

        // Swipe-remove drops the cross-ref.
        repo.removeContactFromGroup(contactId, groupId)
        assertEquals(0, db.contactGroupDao().getMemberCount(groupId).first())

        // UNDO re-adds the same (contactId, groupId) membership.
        repo.addContactToGroup(contactId, groupId)
        val refs = db.contactGroupDao().getAllCrossRefs().first()
        assertEquals(listOf(ContactGroupCrossRef(contactId, groupId)), refs)
        assertEquals(1, db.contactGroupDao().getMemberCount(groupId).first())
    }
}
