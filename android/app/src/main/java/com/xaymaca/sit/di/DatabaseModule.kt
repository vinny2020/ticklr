package com.xaymaca.sit.di

import android.content.Context
import androidx.room.Room
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.dao.MessageTemplateDao
import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.db.SITDatabase
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

    @Provides
    @Singleton
    fun provideSITDatabase(@ApplicationContext context: Context): SITDatabase {
        return Room.databaseBuilder(
            context,
            SITDatabase::class.java,
            "sit_database"
        ).fallbackToDestructiveMigration()
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
