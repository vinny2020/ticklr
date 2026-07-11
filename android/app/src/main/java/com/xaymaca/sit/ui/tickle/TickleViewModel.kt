package com.xaymaca.sit.ui.tickle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.PendingSnackbarMessageStore
import com.xaymaca.sit.service.PendingTickleCompletion
import com.xaymaca.sit.service.PendingTickleCompletionStore
import com.xaymaca.sit.service.PendingTickleOffer
import com.xaymaca.sit.service.PendingTickleOfferStore
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.service.TickleScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TickleViewModel @Inject constructor(
    private val tickleRepository: TickleRepository,
    private val contactRepository: ContactRepository,
    private val contactGroupDao: ContactGroupDao,
    private val pendingTickleCompletionStore: PendingTickleCompletionStore,
    private val pendingTickleOfferStore: PendingTickleOfferStore,
    private val pendingSnackbarMessageStore: PendingSnackbarMessageStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * TIC-84: a pending one-shot save-confirmation snackbar message, stashed at
     * `upsert()` (which runs on [viewModelScope], not the screen's own coroutine
     * scope) and observed by the app-level scaffold (NavGraph) so it survives
     * TickleEditScreen's immediate post-save pop — phone and tablet two-pane
     * alike.
     */
    val pendingSnackbarMessage: StateFlow<String?> = pendingSnackbarMessageStore.pending

    /** Clears the pending snackbar message once it has been surfaced (or is stale). */
    fun consumePendingSnackbarMessage() {
        pendingSnackbarMessageStore.consume()
    }

    /**
     * TIC-82: a pending "mark [name]'s tickle done?" prompt stashed at SMS
     * handoff, observed by the app-level scaffold (NavGraph) so it survives
     * ComposeScreen's pre-handoff pop and shows on return.
     */
    val pendingTickleCompletion: StateFlow<PendingTickleCompletion?> =
        pendingTickleCompletionStore.pending

    /** Clears the pending prompt once it has been surfaced (or is stale). */
    fun consumePendingTickleCompletion() {
        pendingTickleCompletionStore.consume()
    }

    /**
     * TIC-86: a pending "create a tickle for [name]?" offer stashed at a plain
     * SMS handoff (no mark-done reminder applied), observed by the app-level
     * scaffold (NavGraph) so it survives ComposeScreen's pre-handoff pop and the
     * round trip out to the SMS app. Its action navigates to TickleEdit prefilled
     * with the contact — no ViewModel mutation, so there's no completePending-style
     * helper here; NavGraph owns the navigation.
     */
    val pendingTickleOffer: StateFlow<PendingTickleOffer?> =
        pendingTickleOfferStore.pending

    /** Clears the pending offer once it has been surfaced (or is stale). */
    fun consumePendingTickleOffer() {
        pendingTickleOfferStore.consume()
    }

    /**
     * Completes the reminder behind a mark-done prompt, going through the same
     * [markComplete] / [TickleScheduler] path as the in-app actions so alarms
     * stay in sync. Re-validates against the DB: if the reminder was completed
     * or deleted in the meantime, this is a no-op.
     */
    fun completePendingTickle(reminderId: Long) {
        viewModelScope.launch {
            val reminder = tickleRepository.getReminderById(reminderId) ?: return@launch
            if (reminder.status == TickleStatus.COMPLETED.name) return@launch
            markComplete(reminder)
        }
    }

    private val stringListConverter = StringListConverter()

    /** Resolved target for the connect-action sheet shown on tickle tap (TIC-36).
     *  `phones`/`emails` are empty for group tickles (or contacts with no channel). */
    data class TickleActionTarget(
        val reminder: TickleReminder,
        val displayName: String,
        val initials: String,
        val categoryId: String?,
        val phones: List<String>,
        val emails: List<String>,
    )

    private val _actionTarget = MutableStateFlow<TickleActionTarget?>(null)
    val actionTarget: StateFlow<TickleActionTarget?> = _actionTarget.asStateFlow()

    /** Resolves the tapped reminder's contact channels and opens the action sheet. */
    fun onTickleTapped(reminder: TickleReminder) {
        viewModelScope.launch {
            val contact = reminder.contactId?.let { contactRepository.getContactById(it) }
            val display = reminderDisplays.value[reminder.id]
            _actionTarget.value = TickleActionTarget(
                reminder = reminder,
                displayName = display?.name ?: contact?.fullName?.takeIf { it.isNotBlank() } ?: "",
                initials = display?.initials ?: contact?.initials ?: "?",
                categoryId = display?.categoryId,
                phones = contact?.let { stringListConverter.fromString(it.phoneNumbers) } ?: emptyList(),
                emails = contact?.let { stringListConverter.fromString(it.emails) } ?: emptyList(),
            )
        }
    }

    fun dismissActionSheet() {
        _actionTarget.value = null
    }

    val allReminders: StateFlow<List<TickleReminder>> = tickleRepository
        .getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emits the current time immediately, then once a minute while collected.
    // Combining this with the reminders flow makes the Due/Upcoming/Snoozed
    // sections re-evaluate as a reminder crosses its due time — previously `now`
    // was captured only when Room re-emitted, so a reminder wouldn't move
    // sections until some unrelated write happened to refresh the flow.
    private val minuteTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    // Section semantics live in TickleScheduler.isDue/isUpcoming/isSnoozedWaiting
    // (TIC-61): due-ness is date-based, so a snoozed reminder whose snooze
    // window has elapsed surfaces in Due rather than sitting snoozed forever.
    val dueReminders: StateFlow<List<TickleReminder>> = combine(allReminders, minuteTicker) { list, now ->
        list.filter { TickleScheduler.isDue(it, now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<TickleReminder>> = combine(allReminders, minuteTicker) { list, now ->
        list.filter { TickleScheduler.isUpcoming(it, now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snoozedReminders: StateFlow<List<TickleReminder>> = combine(allReminders, minuteTicker) { list, now ->
        list.filter { TickleScheduler.isSnoozedWaiting(it, now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Display data for each reminder row — avatar text, headline name,
     *  and resolved canonical category id (or null for none). */
    data class RowDisplay(
        val initials: String,
        val name: String,
        val categoryId: String? = null,
    )

    /** Maps reminder.id → row display derived from its linked contact or group. */
    val reminderDisplays: StateFlow<Map<Long, RowDisplay>> = combine(
        allReminders,
        contactRepository.getAllContacts(),
        contactRepository.getAllGroups(),
        contactGroupDao.getAllCrossRefs(),
    ) { reminders, contacts, groups, crossRefs ->
        val contactMap = contacts.associateBy { it.id }
        val groupMap = groups.associateBy { it.id }
        // Build contactId → list<groupId> ordered by cross-ref order (insertion).
        val contactToGroupIds: Map<Long, List<Long>> = crossRefs
            .groupBy { it.contactId }
            .mapValues { (_, refs) -> refs.map { it.groupId } }
        reminders.associate { reminder ->
            val display = when {
                reminder.groupId != null -> {
                    val g = groupMap[reminder.groupId]
                    RowDisplay(
                        initials = g?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                        name = g?.name ?: context.getString(R.string.tickle_row_group_fallback),
                        categoryId = g?.categoryId,
                    )
                }
                reminder.contactId != null -> {
                    val c = contactMap[reminder.contactId]
                    // Walk groups in reverse (most-recently-added first) and
                    // take the first canonical one — mirrors iOS resolver.
                    val canonical = contactToGroupIds[reminder.contactId]
                        ?.asReversed()
                        ?.firstNotNullOfOrNull { gid -> groupMap[gid]?.categoryId }
                    RowDisplay(
                        initials = c?.initials ?: "?",
                        name = c?.fullName?.takeIf { it.isNotBlank() } ?: "?",
                        categoryId = canonical,
                    )
                }
                else -> RowDisplay(initials = "T", name = context.getString(R.string.tickle_row_tickle_fallback))
            }
            reminder.id to display
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Resolves the display name used in a reminder's alarm notification. */
    private suspend fun alarmContactName(reminder: TickleReminder): String =
        reminder.contactId?.let { cId ->
            contactRepository.getContactById(cId)?.fullName?.takeIf { it.isNotBlank() }
        } ?: context.getString(R.string.tickle_notification_contact_fallback)

    fun markComplete(reminder: TickleReminder) {
        viewModelScope.launch {
            // TIC-83: the complete transition lives in TickleScheduler so the
            // in-app path and the notification-shade action can't drift.
            val updated = TickleScheduler.completedReminder(reminder)
            tickleRepository.updateReminder(updated)
            // TIC-66: arms the next occurrence's exact alarm (recurring) or
            // cancels the stale one (one-time completed).
            TickleScheduler.syncAlarm(context, updated, alarmContactName(updated))
        }
    }

    fun snooze(reminder: TickleReminder, days: Int = 7) {
        viewModelScope.launch {
            // TIC-83: shared snooze transition — see TickleScheduler.snoozedReminder.
            val updated = TickleScheduler.snoozedReminder(reminder, days)
            tickleRepository.updateReminder(updated)
            // TIC-66: replaces the original-due-date alarm with one at snooze
            // end — previously the stale alarm fired at the old time anyway.
            TickleScheduler.syncAlarm(context, updated, alarmContactName(updated))
        }
    }

    fun delete(reminder: TickleReminder) {
        viewModelScope.launch {
            TickleScheduler.cancelNotification(context, reminder.id)
            tickleRepository.deleteReminder(reminder)
        }
    }

    fun upsert(reminder: TickleReminder, isNew: Boolean) {
        viewModelScope.launch {
            // TIC-84 INVARIANT — NonCancellable is load-bearing, do not
            // "simplify" it away. On the phone flow this ViewModel is scoped to
            // the tickle_edit NavBackStackEntry, and TickleEditScreen calls
            // onSaved() → popBackStack() immediately after upsert() returns (no
            // delay since TIC-84). The pop destroys the back-stack entry,
            // clears its ViewModelStore, and CANCELS viewModelScope — which
            // would abort this coroutine while suspended in the Room write (or
            // before syncAlarm / scheduleWorker / the snackbar post ran):
            // intermittently lost saves, missing alarms, missing confirmation.
            // NonCancellable lets the whole save unit run to completion even
            // after the scope is torn down. Entering the block before the pop
            // is guaranteed: viewModelScope dispatches on Main.immediate, so
            // this body runs synchronously up to its first suspension point —
            // i.e. the NonCancellable context is installed before upsert()
            // returns to the caller, and therefore before onSaved()/the pop
            // (and any cancellation) can happen.
            withContext(NonCancellable) {
                val id = tickleRepository.upsertReminder(reminder)
                val finalId = if (reminder.id == 0L) id else reminder.id
                // TIC-66/67: syncAlarm arms only reminders that will become due
                // (active, or snoozed until snooze-end) and clears stale alarms for
                // completed/past-due ones.
                val saved = reminder.copy(id = finalId)
                TickleScheduler.syncAlarm(context, saved, alarmContactName(saved))
                TickleScheduler.scheduleWorker(context)
                // TIC-84: posted to the app-scoped store (not a screen-local toast)
                // so the confirmation survives TickleEditScreen's immediate pop.
                pendingSnackbarMessageStore.set(
                    if (isNew) context.getString(R.string.tickle_saved) else context.getString(R.string.tickle_updated)
                )
            }
        }
    }

    suspend fun getReminderById(id: Long): TickleReminder? = tickleRepository.getReminderById(id)
}
