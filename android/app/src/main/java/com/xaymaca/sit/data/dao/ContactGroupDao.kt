package com.xaymaca.sit.data.dao

import androidx.room.*
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.GroupWithContacts
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactGroupDao {

    @Query("SELECT * FROM contact_groups ORDER BY name ASC")
    fun getAll(): Flow<List<ContactGroup>>

    @Query("SELECT * FROM contact_groups WHERE id = :id")
    suspend fun getById(id: Long): ContactGroup?

    @Transaction
    @Query("SELECT * FROM contact_groups WHERE id = :id")
    suspend fun getGroupWithContacts(id: Long): GroupWithContacts?

    @Transaction
    @Query("SELECT * FROM contact_groups ORDER BY name ASC")
    fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ContactGroup): Long

    @Update
    suspend fun update(group: ContactGroup)

    @Delete
    suspend fun delete(group: ContactGroup)

    @Query("DELETE FROM contact_groups")
    suspend fun deleteAll()

    @Query("DELETE FROM contact_group_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: ContactGroupCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: ContactGroupCrossRef)

    @Query(
        "SELECT cg.* FROM contact_groups cg " +
        "INNER JOIN contact_group_cross_ref r ON cg.id = r.groupId " +
        "WHERE r.contactId = :contactId " +
        "ORDER BY cg.name ASC"
    )
    fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>>

    @Query("SELECT COUNT(*) FROM contact_group_cross_ref WHERE groupId = :groupId")
    fun getMemberCount(groupId: Long): Flow<Int>
}
