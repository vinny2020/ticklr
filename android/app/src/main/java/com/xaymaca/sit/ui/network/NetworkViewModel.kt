package com.xaymaca.sit.ui.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.service.ContactImportService
import com.xaymaca.sit.service.ContactPhotoService
import com.xaymaca.sit.service.LinkedInCSVParser
import com.xaymaca.sit.service.LocalPhotoStore
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.ui.theme.WarmCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contactImportService: ContactImportService,
    private val linkedInCSVParser: LinkedInCSVParser,
    private val contactDao: ContactDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    /** null = "All" chip; otherwise the WarmCategory.id string. */
    val categoryFilter = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filteredContacts: StateFlow<List<Contact>> = combine(
        searchQuery.debounce(300),
        categoryFilter,
    ) { query, category -> query to category }
        .flatMapLatest { (query, category) ->
            when {
                category != null && query.isBlank() ->
                    contactDao.getContactsInCategory(category)
                category != null ->
                    contactDao.searchContactsInCategory(category, query)
                query.isBlank() ->
                    contactRepository.getAllContacts()
                else ->
                    contactRepository.searchContacts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Per-category counts driving the chip badges. Built once during
     * ViewModel construction so the StateFlows persist across screen
     * recompositions — if we re-call .stateIn() on each chip render
     * the resulting fresh StateFlow starts at 0 every time, causing
     * the chip's `count > 0` visibility predicate to flicker
     * false→true→false until the DB query resolves.
     */
    private val categoryCounts: Map<String, StateFlow<Int>> =
        WarmCategory.values().associate { cat ->
            cat.id to contactDao.countContactsInCategory(cat.id)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        }

    fun countFor(categoryId: String): StateFlow<Int> =
        categoryCounts[categoryId] ?: MutableStateFlow(0).asStateFlow()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setCategoryFilter(categoryId: String?) {
        categoryFilter.value = categoryId
    }

    fun addContact(contact: Contact) {
        viewModelScope.launch { contactRepository.insertContact(contact) }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch { contactRepository.updateContact(contact) }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            // Cancel any armed alarms for this contact's tickles first, otherwise
            // they still fire with the deleted contact's name baked into the intent.
            contactRepository.getRemindersForContact(contact.id).forEach { reminder ->
                TickleScheduler.cancelNotification(context, reminder.id)
            }
            contactRepository.deleteContact(contact)
            // Centralized delete cleanup (TIC-72): both delete entry points
            // (long-press in NetworkListScreen and the ContactDetailScreen
            // button) route through here, so the photo file and the id-keyed
            // photo cache are always cleaned up. Without this, SQLite rowid
            // reuse lets a newly created contact inherit the deleted
            // contact's id — and its orphaned photo file / stale cache entry.
            LocalPhotoStore.delete(context, contact.id)
            ContactPhotoService.evict(contact.id)
        }
    }

    suspend fun importFromContacts(): Int =
        contactImportService.importPhoneContacts()

    suspend fun importFromCSV(inputStream: InputStream): Int {
        val contacts = linkedInCSVParser.parse(inputStream)
        // insertContact returns -1L for duplicates; count only genuine inserts
        // so "Imported N contacts" reflects what actually landed, not re-imports.
        var inserted = 0
        contacts.forEach { if (contactRepository.insertContact(it) != -1L) inserted++ }
        return inserted
    }

    suspend fun getContactById(id: Long): Contact? =
        contactRepository.getContactById(id)

    /**
     * The id of a currently-due reminder for this contact, or null if none.
     * "Due" means past its nextDueDate and still ACTIVE or SNOOZED — the same
     * predicate the Tickle list uses ([TickleScheduler.isDue]). Used by the
     * contact-detail "Send a text" chip (TIC-82) so the compose it opens can
     * prompt to mark that tickle done on return.
     */
    suspend fun dueReminderIdForContact(contactId: Long): Long? {
        val now = System.currentTimeMillis()
        return contactRepository.getRemindersForContact(contactId)
            .firstOrNull { TickleScheduler.isDue(it, now) }
            ?.id
    }
}
