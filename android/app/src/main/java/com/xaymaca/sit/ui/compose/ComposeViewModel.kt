package com.xaymaca.sit.ui.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
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

    val messageBody = MutableStateFlow("")

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val sendDirectly: StateFlow<Boolean> = MutableStateFlow(
        context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)
    )

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
