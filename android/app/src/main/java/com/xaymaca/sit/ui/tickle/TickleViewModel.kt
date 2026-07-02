package com.xaymaca.sit.ui.tickle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.data.dao.ContactGroupDao
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.service.TickleScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TickleViewModel @Inject constructor(
    private val tickleRepository: TickleRepository,
    private val contactRepository: ContactRepository,
    private val contactGroupDao: ContactGroupDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

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

    // Section semantics live in TickleScheduler.isDue/isUpcoming/isSnoozedWaiting
    // (TIC-61): due-ness is date-based, so a snoozed reminder whose snooze
    // window has elapsed surfaces in Due rather than sitting snoozed forever.
    val dueReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { TickleScheduler.isDue(it, now) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { TickleScheduler.isUpcoming(it, now) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snoozedReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { TickleScheduler.isSnoozedWaiting(it, now) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                        name = g?.name ?: "Group",
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
                else -> RowDisplay(initials = "T", name = "Tickle")
            }
            reminder.id to display
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun markComplete(reminder: TickleReminder) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (reminder.frequency == TickleFrequency.ONE_TIME.name) {
                TickleScheduler.cancelNotification(context, reminder.id)
                tickleRepository.updateReminder(
                    reminder.copy(
                        lastCompletedDate = now,
                        status = TickleStatus.COMPLETED.name
                    )
                )
            } else {
                val nextDue = TickleScheduler.nextDueDateOnComplete(
                    frequency = reminder.frequency,
                    startDate = reminder.startDate,
                    customDays = reminder.customIntervalDays,
                    now = now
                )
                tickleRepository.updateReminder(
                    reminder.copy(
                        lastCompletedDate = now,
                        nextDueDate = nextDue,
                        status = TickleStatus.ACTIVE.name
                    )
                )
            }
        }
    }

    fun snooze(reminder: TickleReminder, days: Int = 7) {
        viewModelScope.launch {
            val snoozeUntil = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
            tickleRepository.updateReminder(
                reminder.copy(
                    nextDueDate = snoozeUntil,
                    status = TickleStatus.SNOOZED.name
                )
            )
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
            val id = tickleRepository.upsertReminder(reminder)
            val finalId = if (reminder.id == 0L) id else reminder.id
            val contactName = reminder.contactId?.let { cId ->
                contactRepository.getContactById(cId)?.fullName
            } ?: context.getString(R.string.tickle_notification_contact_fallback)
            // Only schedule an alarm for active reminders. A preserved snoozed or
            // completed reminder (TIC-67) must not fire an alarm for its past due
            // date; clear any stale alarm instead.
            if (reminder.status == TickleStatus.ACTIVE.name) {
                TickleScheduler.scheduleNotification(
                    context, finalId, reminder.contactId, contactName, reminder.note, reminder.nextDueDate
                )
            } else {
                TickleScheduler.cancelNotification(context, finalId)
            }
            TickleScheduler.scheduleWorker(context)
            _toastMessage.value = if (isNew) context.getString(R.string.tickle_saved) else context.getString(R.string.tickle_updated)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    suspend fun getReminderById(id: Long): TickleReminder? = tickleRepository.getReminderById(id)
}
