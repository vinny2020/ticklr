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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: Contact): Long

    @Query("SELECT COUNT(*) FROM contacts WHERE fingerprint = :fingerprint AND fingerprint != ''")
    suspend fun countByFingerprint(fingerprint: String): Int

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

    /** Warm-redesign category filter: returns contacts who are in ANY
     *  group whose canonical categoryId matches the argument. */
    @Query(
        "SELECT DISTINCT c.* FROM contacts c " +
        "INNER JOIN contact_group_cross_ref r ON c.id = r.contactId " +
        "INNER JOIN contact_groups g ON r.groupId = g.id " +
        "WHERE g.categoryId = :categoryId " +
        "ORDER BY c.lastName ASC, c.firstName ASC"
    )
    fun getContactsInCategory(categoryId: String): Flow<List<Contact>>

    /** Same shape as getContactsInCategory but filtered by search query. */
    @Query(
        "SELECT DISTINCT c.* FROM contacts c " +
        "INNER JOIN contact_group_cross_ref r ON c.id = r.contactId " +
        "INNER JOIN contact_groups g ON r.groupId = g.id " +
        "WHERE g.categoryId = :categoryId AND " +
        "(c.firstName LIKE '%' || :query || '%' OR " +
        "c.lastName LIKE '%' || :query || '%' OR " +
        "c.company LIKE '%' || :query || '%') " +
        "ORDER BY c.lastName ASC, c.firstName ASC"
    )
    fun searchContactsInCategory(categoryId: String, query: String): Flow<List<Contact>>

    /** Count of contacts in a canonical category — drives chip badges. */
    @Query(
        "SELECT COUNT(DISTINCT c.id) FROM contacts c " +
        "INNER JOIN contact_group_cross_ref r ON c.id = r.contactId " +
        "INNER JOIN contact_groups g ON r.groupId = g.id " +
        "WHERE g.categoryId = :categoryId"
    )
    fun countContactsInCategory(categoryId: String): Flow<Int>
}
