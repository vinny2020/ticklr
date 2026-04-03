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

    @Query("SELECT * FROM tickle_reminders WHERE nextDueDate <= :now AND status = 'ACTIVE'")
    suspend fun getDueReminders(now: Long): List<TickleReminder>

    @Query("DELETE FROM tickle_reminders")
    suspend fun deleteAll()
}
