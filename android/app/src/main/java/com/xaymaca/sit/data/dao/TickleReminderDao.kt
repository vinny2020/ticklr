package com.xaymaca.sit.data.dao

import androidx.room.*
import com.xaymaca.sit.data.model.TickleReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface TickleReminderDao {

    @Query("SELECT * FROM tickle_reminders ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<TickleReminder>>

    @Query("SELECT * FROM tickle_reminders WHERE id = :id")
    suspend fun getById(id: Long): TickleReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: TickleReminder): Long

    @Update
    suspend fun update(reminder: TickleReminder)

    @Delete
    suspend fun delete(reminder: TickleReminder)

    @Query("SELECT * FROM tickle_reminders WHERE status = :status ORDER BY nextDueDate ASC")
    fun getByStatus(status: String): Flow<List<TickleReminder>>

    // SNOOZED is included: snoozing only pushes nextDueDate out, so once that
    // date passes the reminder is due again (TIC-61). Due-ness is date-based,
    // matching iOS — a status flip back to ACTIVE happens on completion.
    @Query("SELECT * FROM tickle_reminders WHERE nextDueDate <= :now AND status IN ('ACTIVE', 'SNOOZED')")
    suspend fun getDueReminders(now: Long): List<TickleReminder>

    /** Reminders belonging to a contact — used to cancel their alarms before
     *  the contact (and its tickles) are deleted. */
    @Query("SELECT * FROM tickle_reminders WHERE contactId = :contactId")
    suspend fun getByContactId(contactId: Long): List<TickleReminder>

    /** Reminders attached to a group — same purpose as [getByContactId]. */
    @Query("SELECT * FROM tickle_reminders WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: Long): List<TickleReminder>

    @Query("DELETE FROM tickle_reminders WHERE contactId = :contactId")
    suspend fun deleteByContactId(contactId: Long)

    @Query("DELETE FROM tickle_reminders WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: Long)

    @Query("DELETE FROM tickle_reminders")
    suspend fun deleteAll()
}
