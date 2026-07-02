package com.xaymaca.sit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xaymaca.sit.data.db.SITDatabase
import com.xaymaca.sit.di.DatabaseModule
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration regression tests (TIC-60 / TIC-77), Robolectric/JVM — no emulator.
 *
 * Fixtures are hand-built with raw SQL copied from the exported schema JSONs
 * (app/schemas/...), minus the columns later migrations add. Opening the
 * fixture with the production migration chain triggers Room's own
 * post-migration schema validation — the exact code path that crash-looped
 * in production (TIC-60) — so these tests fail if any migration leaves the
 * schema out of sync with the entities.
 */
@RunWith(RobolectricTestRunner::class)
class SITDatabaseMigrationTest {

    private val testDb = "migration-test"
    private lateinit var context: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath(testDb)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
    }

    /** Opens the fixture with the production migration chain and validates it. */
    private fun openWithMigrations(): SITDatabase =
        Room.databaseBuilder(context, SITDatabase::class.java, testDb)
            .addMigrations(*DatabaseModule.ALL_MIGRATIONS)
            .build()

    /**
     * TIC-60 regression: a real v2 database (as Room created it in production)
     * must migrate through the full chain and pass Room's schema validation.
     * Under the original MIGRATION_2_3 — which created a partial unique index
     * the Contact entity no longer declares — this exact scenario threw
     * "Migration didn't properly handle: contacts" and crash-looped the app.
     */
    @Test
    fun fullChainFromV2_opensValidatesAndPreservesData() {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            createV2Schema(db)
            db.execSQL(
                "INSERT INTO contacts (firstName, lastName, phoneNumbers, emails, company, " +
                    "jobTitle, notes, tags, importSource, createdAt, lastContactedAt) " +
                    "VALUES ('Ada', 'Lovelace', '[]', '[]', '', '', '', '[]', 'MANUAL', 1, 1)"
            )
            db.version = 2
        }

        val room = openWithMigrations()
        try {
            // First DAO call forces open → 2→3→4→5 → Room validates every table.
            val migrated = runBlocking { room.contactDao().getById(1L) }
            assertNotNull("v2 contact should survive the migration chain", migrated)
            assertEquals("Ada", migrated!!.firstName)
            assertEquals("", migrated.fingerprint) // v3 column backfilled as unset
        } finally {
            room.close()
        }
    }

    /**
     * Devices that migrated 2→3 under the ORIGINAL code sit at v4 with the
     * orphaned partial unique index. MIGRATION_4_5 must drop it so Room's
     * validation passes instead of crash-looping.
     */
    @Test
    fun migration4To5_dropsOrphanedFingerprintIndex() {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            createV4Schema(db)
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_fingerprint` " +
                    "ON `contacts` (`fingerprint`) WHERE fingerprint != ''"
            )
            db.version = 4
        }

        val room = openWithMigrations()
        try {
            runBlocking { room.contactDao().getById(1L) } // force open + validate
            room.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_contacts_fingerprint'"
            ).use { cursor ->
                assertEquals("orphaned index must be dropped", 0, cursor.count)
            }
        } finally {
            room.close()
        }
    }

    /** Fresh-install-shaped v4 (no index, as created since the entity fix): 4→5 is a clean no-op. */
    @Test
    fun migration4To5_noOpOnFreshInstallShape() {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            createV4Schema(db)
            db.version = 4
        }

        val room = openWithMigrations()
        try {
            runBlocking { room.contactDao().getById(1L) } // open + validate must not throw
        } finally {
            room.close()
        }
    }

    // --- Fixture DDL, transcribed from the exported schema JSONs ---

    /** v4 schema per app/schemas/com.xaymaca.sit.data.db.SITDatabase/4.json. */
    private fun createV4Schema(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, " +
                "`phoneNumbers` TEXT NOT NULL, `emails` TEXT NOT NULL, " +
                "`company` TEXT NOT NULL, `jobTitle` TEXT NOT NULL, " +
                "`notes` TEXT NOT NULL, `tags` TEXT NOT NULL, " +
                "`importSource` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`lastContactedAt` INTEGER NOT NULL, `fingerprint` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `contact_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`categoryId` TEXT)"
        )
        createSharedTables(db)
    }

    /** v2 schema = v4 minus contacts.fingerprint (v3) and contact_groups.categoryId (v4). */
    private fun createV2Schema(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, " +
                "`phoneNumbers` TEXT NOT NULL, `emails` TEXT NOT NULL, " +
                "`company` TEXT NOT NULL, `jobTitle` TEXT NOT NULL, " +
                "`notes` TEXT NOT NULL, `tags` TEXT NOT NULL, " +
                "`importSource` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`lastContactedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `contact_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        createSharedTables(db)
    }

    /** Tables untouched by any migration — identical at every version. */
    private fun createSharedTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `contact_group_cross_ref` (`contactId` INTEGER NOT NULL, " +
                "`groupId` INTEGER NOT NULL, PRIMARY KEY(`contactId`, `groupId`))"
        )
        db.execSQL(
            "CREATE INDEX `index_contact_group_cross_ref_groupId` " +
                "ON `contact_group_cross_ref` (`groupId`)"
        )
        db.execSQL(
            "CREATE TABLE `message_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `tickle_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`contactId` INTEGER, `groupId` INTEGER, `note` TEXT NOT NULL, " +
                "`frequency` TEXT NOT NULL, `customIntervalDays` INTEGER, " +
                "`startDate` INTEGER NOT NULL, `nextDueDate` INTEGER NOT NULL, " +
                "`lastCompletedDate` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
    }
}
