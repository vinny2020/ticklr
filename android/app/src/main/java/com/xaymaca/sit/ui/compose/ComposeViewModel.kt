package com.xaymaca.sit.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val messageTemplateRepository: MessageTemplateRepository,
    private val contactGroupDao: ContactGroupDao,
) : ViewModel() {

    private val allContacts: StateFlow<List<Contact>> = contactRepository
        .getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<MessageTemplate>> = messageTemplateRepository
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")

    val contacts: StateFlow<List<Contact>> = combine(allContacts, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.firstName.contains(query, ignoreCase = true) ||
            it.lastName.contains(query, ignoreCase = true) ||
            it.company.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    /**
     * Resolved canonical categoryId for the currently selected contact
     * (most-recently-added canonical group via reverse-order walk),
     * or null when no contact is selected or the contact isn't in any
     * canonical group. Mirrors the iOS `WarmCategory.resolve(for:)`
     * helper. The Compose screen's Send button + focus accent reads
     * this so it follows the contact's category (Family burgundy,
     * Friends blue, Work forest, etc.) — community fallback handled
     * in the composable.
     */
    val selectedContactCategoryId: StateFlow<String?> = combine(
        _selectedContact,
        contactGroupDao.getAllGroupsWithContacts(),
    ) { contact, groupsWithContacts ->
        val cId = contact?.id ?: return@combine null
        // Walk all groups, build (groupId, categoryId) for canonical
        // ones, then find the LAST canonical group this contact is in.
        groupsWithContacts.asReversed().firstNotNullOfOrNull { gwc ->
            if (gwc.group.categoryId != null && gwc.contacts.any { it.id == cId }) {
                gwc.group.categoryId
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messageBody = MutableStateFlow("")

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val canSend: StateFlow<Boolean> = combine(
        _selectedContact, messageBody
    ) { contact, body ->
        contact != null && contact.phoneNumbers.isNotBlank() &&
            contact.phoneNumbers != "[]" && body.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSearchQuery(query: String) { searchQuery.value = query }

    fun selectContact(contact: Contact) {
        _selectedContact.value = contact
        searchQuery.value = ""
    }

    fun preSelectContact(contactId: Long) {
        viewModelScope.launch {
            val contact = contactRepository.getContactById(contactId)
            if (contact != null) selectContact(contact)
        }
    }

    fun clearContact() {
        _selectedContact.value = null
    }

    /** Stamp the contact as reached-out-to at the moment of SMS handoff. */
    fun recordHandoff(contact: Contact) {
        viewModelScope.launch {
            contactRepository.updateContact(
                contact.copy(lastContactedAt = System.currentTimeMillis())
            )
        }
    }

    fun clearCompose() {
        _selectedContact.value = null
        messageBody.value = ""
    }

    fun setMessage(text: String) {
        messageBody.value = text
    }

    fun applyTemplate(template: MessageTemplate) {
        messageBody.value = template.body
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
