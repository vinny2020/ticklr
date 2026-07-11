package com.xaymaca.sit.ui.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.dao.ContactDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.service.ContactImportService
import com.xaymaca.sit.service.ContactPhotoService
import com.xaymaca.sit.service.ImportResult
import com.xaymaca.sit.service.LinkedInCSVParser
import com.xaymaca.sit.service.LocalPhotoStore
import com.xaymaca.sit.service.PendingSnackbarMessageStore
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.service.formatImportSummaryMessage
import com.xaymaca.sit.ui.theme.WarmCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contactImportService: ContactImportService,
    private val linkedInCSVParser: LinkedInCSVParser,
    private val contactDao: ContactDao,
    private val pendingSnackbarMessageStore: PendingSnackbarMessageStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    /** null = "All" chip; otherwise the WarmCategory.id string. */
    val categoryFilter = MutableStateFlow<String?>(null)
    /**
     * TIC-88: null = no user-group filter; otherwise a ContactGroup.id (Long).
     * Mutually exclusive with [categoryFilter] — selecting one clears the other
     * so the chip row only ever shows one active selection.
     */
    val groupFilter = MutableStateFlow<Long?>(null)

    /** User-created groups (categoryId == null) that back the extra filter chips,
     *  ordered by creation like the Groups list. */
    val userGroups: StateFlow<List<ContactGroup>> = contactRepository
        .getAllGroups()
        .map { groups -> groups.filter { it.categoryId == null }.sortedBy { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filteredContacts: StateFlow<List<Contact>> = combine(
        searchQuery.debounce(300),
        categoryFilter,
        groupFilter,
    ) { query, category, group -> Triple(query, category, group) }
        .flatMapLatest { (query, category, group) ->
            when {
                // Group filter wins when set; member search is applied in-memory
                // (memberships are small) via the pure [NetworkGroupFilter].
                group != null && query.isBlank() ->
                    contactRepository.getContactsForGroup(group)
                group != null ->
                    contactRepository.getContactsForGroup(group)
                        .map { NetworkGroupFilter.filter(it, query) }
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
        // Canonical + user-group filters are mutually exclusive.
        if (categoryId != null) groupFilter.value = null
    }

    /** TIC-88: select (or clear) a user-group filter, clearing any canonical
     *  category selection so only one chip is ever active. */
    fun setGroupFilter(groupId: Long?) {
        groupFilter.value = groupId
        if (groupId != null) categoryFilter.value = null
    }

    // NonCancellable on the three mutations below is load-bearing (found via
    // TIC-84) — do not "simplify" it away. Each is fired from a screen that
    // pops its own NavBackStackEntry in the same tick as the call
    // (AddContactScreen's save → onSaved() → popBackStack(); ContactDetail's
    // delete confirm → onBack()). On the phone flow this ViewModel is scoped
    // to that entry, so the pop clears the ViewModelStore and CANCELS
    // viewModelScope — without the guard, the coroutine aborts mid-Room-write
    // and the save/delete is intermittently lost. NonCancellable lets the
    // whole unit run to completion after teardown; it is always entered
    // before the pop because viewModelScope dispatches on Main.immediate
    // (the body runs synchronously up to its first suspension point, i.e.
    // before the calling function returns to the screen).

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                contactRepository.insertContact(contact)
            }
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                contactRepository.updateContact(contact)
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            withContext(NonCancellable) {
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
    }

    /**
     * Imports device contacts and stashes the result summary on the app-scoped
     * [PendingSnackbarMessageStore] (TIC-84 pattern) before returning — the
     * Import screen (onboarding, or a mid-session import from Settings/Network)
     * auto-advances to the Network tab as soon as this returns (TIC-85), so the
     * message has to be posted here rather than built from a screen-local
     * count that would already be gone by the time it's shown.
     */
    suspend fun importFromContacts(): ImportResult {
        val result = contactImportService.importPhoneContacts()
        pendingSnackbarMessageStore.set(formatImportSummaryMessage(context, result))
        return result
    }

    /** CSV counterpart of [importFromContacts] — same TIC-85 summary-posting contract. */
    suspend fun importFromCSV(inputStream: InputStream): ImportResult {
        val contacts = linkedInCSVParser.parse(inputStream)
        // insertContact returns -1L for duplicates; tally both so the summary
        // can report genuine inserts and duplicates skipped separately.
        var inserted = 0
        var skipped = 0
        contacts.forEach { if (contactRepository.insertContact(it) != -1L) inserted++ else skipped++ }
        val result = ImportResult(inserted, skipped)
        pendingSnackbarMessageStore.set(formatImportSummaryMessage(context, result))
        return result
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

/**
 * TIC-88: pure member-search predicate for the user-group Network filter. The
 * group's members come from the DB; a non-blank query narrows them in-memory on
 * name or company (case-insensitive), mirroring the DAO category search. Kept
 * standalone so the filtering is unit-testable without Room.
 */
internal object NetworkGroupFilter {
    fun filter(members: List<Contact>, query: String): List<Contact> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return members
        return members.filter {
            it.fullName.contains(trimmed, ignoreCase = true) ||
                it.company.contains(trimmed, ignoreCase = true)
        }
    }
}
