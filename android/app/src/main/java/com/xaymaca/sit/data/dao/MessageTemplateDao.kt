package com.xaymaca.sit.data.dao

import androidx.room.*
import com.xaymaca.sit.data.model.MessageTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageTemplateDao {

    @Query("SELECT * FROM message_templates ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MessageTemplate>>

    @Query("SELECT * FROM message_templates WHERE id = :id")
    suspend fun getById(id: Long): MessageTemplate?

    /** Snapshot count, used by the seed to decide whether to insert
     *  the default template even when the SharedPrefs flag is set. */
    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MessageTemplate): Long

    @Update
    suspend fun update(template: MessageTemplate)

    @Delete
    suspend fun delete(template: MessageTemplate)
}
