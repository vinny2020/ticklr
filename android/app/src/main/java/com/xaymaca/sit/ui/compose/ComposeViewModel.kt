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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val messageTemplateRepository: MessageTemplateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    enum class SortOrder { AZ, ZA, RECENT }

    private val allContacts: StateFlow<List<Contact>> = contactRepository
        .getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<MessageTemplate>> = messageTemplateRepository
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.AZ)

    val contacts: StateFlow<List<Contact>> = combine(allContacts, searchQuery, sortOrder) { list, query, sort ->
        val filtered = if (query.isBlank()) list
        else list.filter {
            it.firstName.contains(query, ignoreCase = true) ||
            it.lastName.contains(query, ignoreCase = true) ||
            it.company.contains(query, ignoreCase = true)
        }
        when (sort) {
            SortOrder.AZ     -> filtered.sortedBy { it.lastName.lowercase() }
            SortOrder.ZA     -> filtered.sortedByDescending { it.lastName.lowercase() }
            SortOrder.RECENT -> filtered.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedContactIds = MutableStateFlow<Set<Long>>(emptySet())
    val messageBody = MutableStateFlow("")

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val sendDirectly: StateFlow<Boolean> = MutableStateFlow(
        context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)
    )

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setSortOrder(order: SortOrder) { sortOrder.value = order }

    fun toggleContactSelection(contactId: Long) {
        val current = selectedContactIds.value.toMutableSet()
        if (current.contains(contactId)) {
            current.remove(contactId)
        } else {
            current.add(contactId)
        }
        selectedContactIds.value = current
    }

    fun setMessage(text: String) {
        messageBody.value = text
    }

    fun applyTemplate(template: MessageTemplate) {
        messageBody.value = template.body
    }

    fun clearSelection() {
        selectedContactIds.value = emptySet()
        messageBody.value = ""
    }

    fun saveTemplate(title: String, body: String) {
        viewModelScope.launch {
            messageTemplateRepository.insertTemplate(
                MessageTemplate(title = title.trim(), body = body.trim())
            )
            _toastMessage.value = "Template saved"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun deleteTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            messageTemplateRepository.deleteTemplate(template)
        }
    }
}
