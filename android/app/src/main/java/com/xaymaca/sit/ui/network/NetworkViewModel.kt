package com.xaymaca.sit.ui.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.service.ContactImportService
import com.xaymaca.sit.service.LinkedInCSVParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contactImportService: ContactImportService,
    private val linkedInCSVParser: LinkedInCSVParser
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filteredContacts: StateFlow<List<Contact>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                contactRepository.getAllContacts()
            } else {
                contactRepository.searchContacts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.insertContact(contact)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
        }
    }

    /**
     * Imports contacts from the phone's contacts.
     * Caller must ensure READ_CONTACTS permission is granted.
     * Returns the number of contacts imported.
     */
    suspend fun importFromContacts(): Int {
        return contactImportService.importPhoneContacts()
    }

    /**
     * Parses and imports a LinkedIn CSV from the given InputStream.
     * Returns the number of contacts imported.
     */
    suspend fun importFromCSV(inputStream: InputStream): Int {
        val contacts = linkedInCSVParser.parse(inputStream)
        contacts.forEach { contact ->
            contactRepository.insertContact(contact)
        }
        return contacts.size
    }

    suspend fun getContactById(id: Long): Contact? = contactRepository.getContactById(id)
}
