package com.xaymaca.sit.data.repository

import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.ContactGroupCrossRef
import com.xaymaca.sit.data.model.ContactWithGroups
import com.xaymaca.sit.data.model.GroupWithContacts
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
    private val contactGroupDao: ContactGroupDao
) {
    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAll()

    fun searchContacts(query: String): Flow<List<Contact>> = contactDao.search(query)

    fun getContactsForGroup(groupId: Long): Flow<List<Contact>> =
        contactDao.getContactsForGroup(groupId)

    suspend fun getContactById(id: Long): Contact? = contactDao.getById(id)

    suspend fun getContactWithGroups(id: Long): ContactWithGroups? =
        contactDao.getContactWithGroups(id)

    suspend fun insertContact(contact: Contact): Long = contactDao.insert(contact)

    suspend fun updateContact(contact: Contact) = contactDao.update(contact)

    suspend fun deleteContact(contact: Contact) = contactDao.delete(contact)

    suspend fun deleteAllContacts() = contactDao.deleteAll()

    suspend fun deleteAllGroups() {
        contactGroupDao.deleteAllCrossRefs()
        contactGroupDao.deleteAll()
    }

    // Groups
    fun getAllGroups(): Flow<List<ContactGroup>> = contactGroupDao.getAll()

    fun getAllGroupsWithContacts(): Flow<List<GroupWithContacts>> =
        contactGroupDao.getAllGroupsWithContacts()

    fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> =
        contactGroupDao.getGroupsForContact(contactId)

    suspend fun getGroupById(id: Long): ContactGroup? = contactGroupDao.getById(id)

    suspend fun getGroupWithContacts(id: Long): GroupWithContacts? =
        contactGroupDao.getGroupWithContacts(id)

    suspend fun insertGroup(group: ContactGroup): Long = contactGroupDao.insert(group)

    suspend fun updateGroup(group: ContactGroup) = contactGroupDao.update(group)

    suspend fun deleteGroup(group: ContactGroup) = contactGroupDao.delete(group)

    suspend fun addContactToGroup(contactId: Long, groupId: Long) {
        contactGroupDao.insertCrossRef(ContactGroupCrossRef(contactId, groupId))
    }

    suspend fun removeContactFromGroup(contactId: Long, groupId: Long) {
        contactGroupDao.deleteCrossRef(ContactGroupCrossRef(contactId, groupId))
    }

    fun getGroupMemberCount(groupId: Long): Flow<Int> =
        contactGroupDao.getMemberCount(groupId)
}
