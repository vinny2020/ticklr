package com.xaymaca.sit.ui.groups

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.service.TickleScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val groups: StateFlow<List<ContactGroup>> = contactRepository
        .getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupsWithContacts: StateFlow<List<GroupWithContacts>> = contactRepository
        .getAllGroupsWithContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContacts: StateFlow<List<Contact>> = contactRepository
        .getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
        viewModelScope.launch {
            delay(2000)
            _toastMessage.value = null
        }
    }

    fun createGroup(name: String, emoji: String) {
        viewModelScope.launch {
            contactRepository.insertGroup(
                ContactGroup(name = name.trim(), emoji = emoji.trim().ifBlank { "👥" })
            )
        }
    }

    /**
     * Returns true if any existing group has the same name (case-insensitive).
     * Pass [excludeId] when editing so the group's own current name is not flagged.
     *
     * Queries the DAO directly rather than reading the [groups] StateFlow, since callers
     * (create/rename dialogs) don't necessarily collect that flow and would otherwise see
     * a permanently-empty snapshot.
     */
    suspend fun isGroupNameTaken(name: String, excludeId: Long = -1L): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return contactRepository.isGroupNameTaken(trimmed, excludeId)
    }

    fun updateGroup(group: ContactGroup) {
        viewModelScope.launch {
            contactRepository.updateGroup(group)
        }
    }

    fun deleteGroup(group: ContactGroup) {
        viewModelScope.launch {
            // Cancel armed alarms for this group's tickles before deleting them,
            // otherwise they still fire referencing the deleted group.
            contactRepository.getRemindersForGroup(group.id).forEach { reminder ->
                TickleScheduler.cancelNotification(context, reminder.id)
            }
            contactRepository.deleteGroup(group)
        }
    }

    fun addMember(contactId: Long, groupId: Long) {
        viewModelScope.launch {
            contactRepository.addContactToGroup(contactId, groupId)
        }
    }

    fun removeMember(contactId: Long, groupId: Long) {
        viewModelScope.launch {
            contactRepository.removeContactFromGroup(contactId, groupId)
        }
    }

    fun createGroupAndAddContact(groupName: String, contactId: Long) {
        viewModelScope.launch {
            val groupId = contactRepository.insertGroup(
                ContactGroup(name = groupName.trim(), emoji = "👥")
            )
            contactRepository.addContactToGroup(contactId, groupId)
        }
    }

    fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>> =
        contactRepository.getGroupsForContact(contactId)

    suspend fun getGroupById(id: Long): ContactGroup? = contactRepository.getGroupById(id)

    suspend fun getGroupWithContacts(id: Long): GroupWithContacts? =
        contactRepository.getGroupWithContacts(id)
}
