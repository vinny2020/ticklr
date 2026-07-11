package com.xaymaca.sit.data.repository

import com.xaymaca.sit.data.dao.TickleReminderDao
import com.xaymaca.sit.data.model.TickleReminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TickleRepository @Inject constructor(
    private val tickleReminderDao: TickleReminderDao
) {
    fun getAllReminders(): Flow<List<TickleReminder>> = tickleReminderDao.getAll()

    fun getRemindersByStatus(status: String): Flow<List<TickleReminder>> =
        tickleReminderDao.getByStatus(status)

    suspend fun getReminderById(id: Long): TickleReminder? = tickleReminderDao.getById(id)

    /** All reminders attached to a contact, any status (TIC-86 duplicate-offer check). */
    suspend fun getRemindersForContact(contactId: Long): List<TickleReminder> =
        tickleReminderDao.getByContactId(contactId)

    suspend fun getDueReminders(now: Long = System.currentTimeMillis()): List<TickleReminder> =
        tickleReminderDao.getDueReminders(now)

    /** Reminders whose exact alarm should be armed — see [TickleReminderDao.getArmableReminders]. */
    suspend fun getArmableReminders(now: Long = System.currentTimeMillis()): List<TickleReminder> =
        tickleReminderDao.getArmableReminders(now)

    suspend fun upsertReminder(reminder: TickleReminder): Long =
        tickleReminderDao.insert(reminder)

    suspend fun updateReminder(reminder: TickleReminder) = tickleReminderDao.update(reminder)

    suspend fun deleteReminder(reminder: TickleReminder) = tickleReminderDao.delete(reminder)

    suspend fun deleteAllReminders() = tickleReminderDao.deleteAll()
}
