package com.xaymaca.sit.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.dao.MessageTemplateDao
import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.model.TickleReminder

@TypeConverters(StringListConverter::class)
@Database(
    entities = [
        Contact::class,
        ContactGroup::class,
        ContactGroupCrossRef::class,
        MessageTemplate::class,
        TickleReminder::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SITDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun contactGroupDao(): ContactGroupDao
    abstract fun messageTemplateDao(): MessageTemplateDao
    abstract fun tickleReminderDao(): TickleReminderDao
}
