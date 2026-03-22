package com.xaymaca.sit.data.dao

import androidx.room.*
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactWithGroups
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY lastName ASC, firstName ASC")
    fun getAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): Contact?

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactWithGroups(id: Long): ContactWithGroups?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query(
        "SELECT * FROM contacts WHERE " +
        "firstName LIKE '%' || :query || '%' OR " +
        "lastName LIKE '%' || :query || '%' OR " +
        "company LIKE '%' || :query || '%' " +
        "ORDER BY lastName ASC, firstName ASC"
    )
    fun search(query: String): Flow<List<Contact>>

    @Query(
        "SELECT c.* FROM contacts c " +
        "INNER JOIN contact_group_cross_ref r ON c.id = r.contactId " +
        "WHERE r.groupId = :groupId " +
        "ORDER BY c.lastName ASC, c.firstName ASC"
    )
    fun getContactsForGroup(groupId: Long): Flow<List<Contact>>
}
