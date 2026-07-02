package com.xaymaca.sit.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.dao.MessageTemplateDao
import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.db.SITDatabase
import com.xaymaca.sit.BuildConfig
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import com.xaymaca.sit.data.repository.TickleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * v2 → v3: Add fingerprint column for contact deduplication. Existing contacts
     * get an empty fingerprint ("") which is treated as unset — they remain in the
     * DB untouched and will get fingerprinted on next edit/re-import.
     *
     * TIC-60: this migration originally also created a partial unique index
     * (index_contacts_fingerprint). The @Index was later removed from the Contact
     * entity, so Room's post-migration schema validation expects NO indices on
     * contacts — creating one here guaranteed a crash loop on the v2 upgrade path.
     * Dedup is enforced in ContactRepository.insertContact via countByFingerprint,
     * not by an index. MIGRATION_4_5 drops the index from devices that already
     * migrated under the original code.
     *
     * Migrations are non-private so SITDatabaseMigrationTest can exercise the
     * exact chain production uses.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN fingerprint TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * v3 → v4: Add `categoryId` column to contact_groups so the warm-
     * redesign canonical groups (Family / Close Friends / Work /
     * Milestones / Neighbors & Community) can be identified stably.
     * Existing rows get NULL (treated as user-created groups in the
     * warm UI).
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE contact_groups ADD COLUMN categoryId TEXT DEFAULT NULL")
        }
    }

    /**
     * v4 → v5: Drop the orphaned partial unique index that the original v2→v3
     * migration created (TIC-60). Devices that migrated before the fix still
     * carry it and fail Room's schema validation on every open — a launch
     * crash loop. No-op for fresh installs (which never had the index).
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_contacts_fingerprint")
        }
    }

    /** Single source of truth for the migration chain — used by the provider and by tests. */
    val ALL_MIGRATIONS = arrayOf<Migration>(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

    @Provides
    @Singleton
    fun provideSITDatabase(@ApplicationContext context: Context): SITDatabase {
        return Room.databaseBuilder(
            context,
            SITDatabase::class.java,
            "sit_database"
        )
            .addMigrations(*ALL_MIGRATIONS)
            .apply {
                // Debug-only convenience. In production the on-device DB is the ONLY
                // copy of the user's data — a missing migration must fail loudly,
                // never silently wipe.
                if (BuildConfig.DEBUG) {
                    fallbackToDestructiveMigration(dropAllTables = true)
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideContactDao(db: SITDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideContactGroupDao(db: SITDatabase): ContactGroupDao = db.contactGroupDao()

    @Provides
    @Singleton
    fun provideMessageTemplateDao(db: SITDatabase): MessageTemplateDao = db.messageTemplateDao()

    @Provides
    @Singleton
    fun provideTickleReminderDao(db: SITDatabase): TickleReminderDao = db.tickleReminderDao()

    @Provides
    @Singleton
    fun provideContactRepository(
        contactDao: ContactDao,
        contactGroupDao: ContactGroupDao
    ): ContactRepository = ContactRepository(contactDao, contactGroupDao)

    @Provides
    @Singleton
    fun provideTickleRepository(tickleReminderDao: TickleReminderDao): TickleRepository =
        TickleRepository(tickleReminderDao)

    @Provides
    @Singleton
    fun provideMessageTemplateRepository(messageTemplateDao: MessageTemplateDao): MessageTemplateRepository =
        MessageTemplateRepository(messageTemplateDao)
}
