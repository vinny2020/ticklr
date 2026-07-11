package com.xaymaca.sit.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.PendingTickleCompletion
import com.xaymaca.sit.service.PendingTickleCompletionStore
import com.xaymaca.sit.service.PendingTickleOffer
import com.xaymaca.sit.service.PendingTickleOfferStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val tickleRepository: TickleRepository,
    private val pendingTickleCompletionStore: PendingTickleCompletionStore,
    private val pendingTickleOfferStore: PendingTickleOfferStore,
) : ViewModel() {

    // TIC-82: the due tickle this compose was opened for, and the contact it
    // belongs to. Held here (not in composable state) so a recipient swap can
    // be detected at send time — if the user re-targets the message to someone
    // else, the reminder no longer applies and we don't prompt to complete it.
    private var pendingReminderId: Long? = null
    private var pendingReminderContactId: Long? = null

    /**
     * Attach the due reminder (if any) this compose was launched for. Called by
     * ComposeScreen from the reminder id threaded through its entry point
     * (tickle action sheet, notification deep link, or contact-detail chip).
     */
    fun attachReminder(reminderId: Long?, contactId: Long?) {
        pendingReminderId = reminderId?.takeIf { it != -1L }
        pendingReminderContactId = contactId?.takeIf { it != -1L }
    }

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

    private val allReminders: StateFlow<List<TickleReminder>> = tickleRepository
        .getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * TIC-86: browse-mode suggestions for the "To" field when it's focused with an
     * empty query — Due today (currently-due reminders, most overdue first),
     * Recents (by lastContactedAt), and the full alphabetical browse list. Assembly
     * lives in the pure [RecipientSuggestionAssembler] so the ordering/cap/exclusion
     * rules are unit-tested; the ViewModel just supplies the live data + clock.
     */
    val recipientSuggestions: StateFlow<RecipientSuggestions> =
        combine(allContacts, allReminders) { contacts, reminders ->
            RecipientSuggestionAssembler.assemble(contacts, reminders, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipientSuggestions.EMPTY)

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

    /**
     * Stamp the contact as reached-out-to at the moment of SMS handoff, and stash
     * at most ONE follow-up prompt for the return to the app — three outcomes:
     *
     * 1. **Mark-done prompt** (TIC-82): this compose carried a still-valid
     *    reminder for THIS recipient. Skipped when the reminder was completed or
     *    deleted in the meantime (validated against the DB, mirroring TIC-66's
     *    shouldPostFiredAlarm philosophy) or the message was re-targeted.
     * 2. **Create-a-tickle offer** (TIC-86): no mark-done prompt applies AND the
     *    recipient has no non-COMPLETED reminder — "text now → remind me later".
     * 3. **Nothing**: no mark-done prompt applies but the recipient already has
     *    an ACTIVE/SNOOZED reminder (just not the one this compose carried, or
     *    not due right now) — offering would invite creating a duplicate tickle
     *    for someone already covered.
     */
    fun recordHandoff(contact: Contact) {
        viewModelScope.launch {
            // see TickleViewModel.upsert — TIC-84 invariant: ComposeScreen pops
            // (onDone()) right after the handoff and this ViewModel is
            // destination-scoped, so the pop cancels viewModelScope while this
            // coroutine is suspended in the Room write (or before the
            // prompt/offer stash runs). NonCancellable lets the lastContactedAt
            // stamp + DB validation + stash run to completion.
            withContext(NonCancellable) {
                contactRepository.updateContact(
                    contact.copy(lastContactedAt = System.currentTimeMillis())
                )
                val contactName = contact.fullName.ifBlank { "" }
                val reminderId = pendingReminderId
                var stashedMarkDone = false
                if (reminderId != null && contact.id == pendingReminderContactId) {
                    val reminder = tickleRepository.getReminderById(reminderId)
                    if (reminder != null && reminder.status != TickleStatus.COMPLETED.name) {
                        pendingTickleCompletionStore.set(
                            PendingTickleCompletion(
                                reminderId = reminderId,
                                contactName = contactName,
                            )
                        )
                        stashedMarkDone = true
                    }
                }
                if (!stashedMarkDone) {
                    // Outcome 3 guard: don't offer a tickle for a contact who
                    // already has a live (non-COMPLETED) reminder.
                    val alreadyCovered = tickleRepository
                        .getRemindersForContact(contact.id)
                        .any { it.status != TickleStatus.COMPLETED.name }
                    if (!alreadyCovered) {
                        pendingTickleOfferStore.set(
                            PendingTickleOffer(
                                contactId = contact.id,
                                contactName = contactName,
                            )
                        )
                    }
                }
                // One-shot: once handed off, don't re-prompt for a later send in
                // the same compose session unless a new reminder is attached.
                pendingReminderId = null
                pendingReminderContactId = null
            }
        }
    }

    fun clearCompose() {
        _selectedContact.value = null
        messageBody.value = ""
        lastAppliedTemplateBody = null
    }

    fun setMessage(text: String) {
        messageBody.value = text
        // Any edit that leaves the body reading differently than the last
        // template applied means it's no longer a safe-to-overwrite draft.
        if (text != lastAppliedTemplateBody) lastAppliedTemplateBody = null
    }

    // TIC-90: the body of the most recently applied template, so a second
    // template application can tell an untouched draft (safe to overwrite)
    // apart from user-typed text (needs a confirm prompt first). Cleared by
    // setMessage/clearCompose whenever the body no longer matches it.
    private var lastAppliedTemplateBody: String? = null

    /**
     * TIC-90: whether applying [MessageTemplate] right now would silently
     * clobber user-typed text. ComposeScreen calls this before applying a
     * template and shows a "Replace your draft?" confirmation when true.
     */
    fun shouldConfirmTemplateReplace(): Boolean =
        TemplateApplyDecision.shouldConfirmReplace(messageBody.value, lastAppliedTemplateBody)

    fun applyTemplate(template: MessageTemplate) {
        messageBody.value = template.body
        lastAppliedTemplateBody = template.body
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
