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

    val allReminders: StateFlow<List<TickleReminder>> = tickleRepository
        .getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.status == TickleStatus.ACTIVE.name && it.nextDueDate <= now }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.status == TickleStatus.ACTIVE.name && it.nextDueDate > now }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snoozedReminders: StateFlow<List<TickleReminder>> = allReminders
        .map { list -> list.filter { it.status == TickleStatus.SNOOZED.name } }
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
            val nextDue = TickleScheduler.nextDueDate(
                from = System.currentTimeMillis(),
                frequency = reminder.frequency,
                customDays = reminder.customIntervalDays
            )
            tickleRepository.updateReminder(
                reminder.copy(
                    lastCompletedDate = System.currentTimeMillis(),
                    nextDueDate = nextDue,
                    status = TickleStatus.ACTIVE.name
                )
            )
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
            TickleScheduler.scheduleNotification(
                context, finalId, contactName, reminder.note, reminder.nextDueDate
            )
            TickleScheduler.scheduleWorker(context)
            _toastMessage.value = if (isNew) context.getString(R.string.tickle_saved) else context.getString(R.string.tickle_updated)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    suspend fun getReminderById(id: Long): TickleReminder? = tickleRepository.getReminderById(id)
}
