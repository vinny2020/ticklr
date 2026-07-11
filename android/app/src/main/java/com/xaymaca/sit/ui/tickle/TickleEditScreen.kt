package com.xaymaca.sit.ui.tickle

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.data.model.TickleStatus
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.ui.network.NetworkViewModel
import com.xaymaca.sit.ui.shared.displayNameResId
import com.xaymaca.sit.ui.theme.WarmCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * TIC-88: resolves the (contactId, groupId) a saved tickle should carry. A
 * group-bound tickle stores its groupId with a null contact; a contact-bound one
 * the reverse. Pure so the mutual exclusion is unit-testable without the form.
 */
internal fun tickleBinding(groupBound: Boolean, contactId: Long?, groupId: Long?): Pair<Long?, Long?> =
    if (groupBound) null to groupId else contactId to null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleEditScreen(
    tickleId: Long?,
    preselectedContactId: Long? = null,
    preselectedGroupId: Long? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    tickleViewModel: TickleViewModel = hiltViewModel(),
    networkViewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contacts by networkViewModel.filteredContacts.collectAsState()

    // TIC-88: a tickle is either contact-bound or group-bound. `groupBound` is
    // decided synchronously (a preselected group for a NEW tickle, or an edited
    // reminder that carries a groupId) so the contact picker never flashes before
    // [boundGroup] finishes loading. When group-bound, Save wires groupId and
    // leaves contactId null; the contact search/list is hidden entirely.
    var groupBound by remember { mutableStateOf(preselectedGroupId != null) }
    var boundGroup by remember { mutableStateOf<ContactGroup?>(null) }

    // Form state — contact only. Group-based tickles bind via [boundGroup] above.
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var contactSearch by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(TickleFrequency.MONTHLY) }
    var customIntervalDays by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(tickleId == null) }
    var isSaving by remember { mutableStateOf(false) }

    // Pre-populate contact when navigating from ContactDetail
    LaunchedEffect(preselectedContactId) {
        if (tickleId == null && preselectedContactId != null) {
            selectedContact = networkViewModel.getContactById(preselectedContactId)
        }
    }

    // TIC-88: pre-populate the bound group when navigating from GroupDetail.
    LaunchedEffect(preselectedGroupId) {
        if (tickleId == null && preselectedGroupId != null) {
            boundGroup = tickleViewModel.getGroupById(preselectedGroupId)
        }
    }

    // Load existing reminder for edit mode. TIC-88: a reminder that carries a
    // groupId re-opens group-bound (contact picker hidden); a contact reminder
    // re-selects its contact. Frequency / note / startDate always carry over.
    LaunchedEffect(tickleId) {
        if (tickleId != null) {
            val reminder = tickleViewModel.getReminderById(tickleId)
            if (reminder != null) {
                selectedFrequency = TickleFrequency.entries.firstOrNull {
                    it.name == reminder.frequency
                } ?: TickleFrequency.MONTHLY
                customIntervalDays = reminder.customIntervalDays ?: 14
                note = reminder.note
                startDate = reminder.startDate
                if (reminder.groupId != null) {
                    groupBound = true
                    boundGroup = tickleViewModel.getGroupById(reminder.groupId)
                } else if (reminder.contactId != null) {
                    selectedContact = networkViewModel.getContactById(reminder.contactId)
                }
            }
            isLoaded = true
        }
    }

    val filteredContacts = remember(contacts, contactSearch) {
        if (contactSearch.isBlank()) contacts
        else contacts.filter {
            it.fullName.contains(contactSearch, ignoreCase = true) ||
            it.company.contains(contactSearch, ignoreCase = true)
        }
    }
    val formattedStartDate = remember(startDate) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(startDate))
    }
    fun showStartDatePicker() {
        val selected = Calendar.getInstance().apply { timeInMillis = startDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (tickleId == null) R.string.tickle_edit_new_title else R.string.tickle_edit_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // Group-bound: savable once its group has resolved. Contact-bound:
                    // needs a picked contact. Either way, wait for load + no in-flight save.
                    val canSave = isLoaded && !isSaving &&
                        (if (groupBound) boundGroup != null else selectedContact != null)
                    TextButton(
                        onClick = {
                            // Flip OUTSIDE coroutineScope.launch so the next tap
                            // sees canSave == false on the next recomposition. If
                            // set inside the coroutine, recomposition lags and rapid
                            // taps each launch a fresh insert.
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val newCustomDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null
                                    val original = if (tickleId != null) tickleViewModel.getReminderById(tickleId) else null
                                    val nextDue = TickleScheduler.nextDueDateForSave(
                                        original = original,
                                        frequency = selectedFrequency.name,
                                        customDays = newCustomDays,
                                        startDate = startDate
                                    )
                                    // Empty (not whitespace-only) defaults to the localized
                                    // "Stay in touch" — saves users from blank-noted reminders
                                    // without overriding intentional whitespace edits.
                                    val finalNote = if (note.isEmpty()) context.getString(R.string.tickle_edit_default_note) else note.trim()
                                    val (savedContactId, savedGroupId) = tickleBinding(
                                        groupBound = groupBound,
                                        contactId = selectedContact?.id,
                                        groupId = boundGroup?.id,
                                    )
                                    val reminder = if (original != null) {
                                        // TIC-67: copy-from-original so a note/contact edit keeps
                                        // lastCompletedDate + createdAt (REPLACE would wipe them).
                                        // Only reset status when a schedule field changed — otherwise
                                        // preserve it so a typo fix doesn't un-snooze the reminder.
                                        val scheduleChanged = TickleScheduler.scheduleChanged(
                                            original = original,
                                            frequency = selectedFrequency.name,
                                            customDays = newCustomDays,
                                            startDate = startDate
                                        )
                                        original.copy(
                                            contactId = savedContactId,
                                            groupId = savedGroupId,
                                            note = finalNote,
                                            frequency = selectedFrequency.name,
                                            customIntervalDays = newCustomDays,
                                            startDate = startDate,
                                            nextDueDate = nextDue,
                                            status = if (scheduleChanged) TickleStatus.ACTIVE.name else original.status
                                        )
                                    } else {
                                        TickleReminder(
                                            id = 0L,
                                            contactId = savedContactId,
                                            groupId = savedGroupId,
                                            note = finalNote,
                                            frequency = selectedFrequency.name,
                                            customIntervalDays = newCustomDays,
                                            startDate = startDate,
                                            nextDueDate = nextDue
                                        )
                                    }
                                    // TIC-84: save applies immediately and navigation
                                    // happens immediately — no artificial delay.
                                    // upsert() itself runs on the ViewModel's
                                    // viewModelScope (not this screen's coroutineScope),
                                    // so the DB write / alarm sync / save-confirmation
                                    // snackbar all complete even after onSaved() pops
                                    // this screen off the back stack.
                                    tickleViewModel.upsert(reminder, isNew = tickleId == null)
                                    onSaved()
                                    // Deliberately NOT resetting isSaving here. With the
                                    // delay(2000) gone there's no suspension point left
                                    // between the flip-true above and this line — upsert()
                                    // isn't suspend (it fires its own viewModelScope
                                    // coroutine and returns) and onSaved() is a plain
                                    // callback, so a `finally { isSaving = false }` would
                                    // flip the guard back off before a rapid second/third
                                    // tap's recomposition ever saw it disabled, letting
                                    // each tap launch its own insert (the exact bug this
                                    // guard exists to prevent). onSaved() is popping this
                                    // screen (or, in the two-pane detail, swapping it out
                                    // of composition) regardless, so there's no user-facing
                                    // button left to re-enable on the success path.
                                } catch (e: CancellationException) {
                                    // Composable was disposed (e.g. the pop already tore
                                    // down this coroutineScope) — nothing left to reset.
                                    throw e
                                } catch (e: Exception) {
                                    // Genuine failure: re-enable so the user can retry.
                                    isSaving = false
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Text(
                            stringResource(R.string.common_save),
                            color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // TIC-88: group-bound tickles pin a read-only group header instead
                // of the contact search — the target is fixed, so there's nothing
                // to pick. Contact-bound tickles keep the search field pinned
                // outside the LazyColumn so it (and its result rows) survive the IME.
                if (groupBound) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.tickle_edit_section_group),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BoundGroupChip(group = boundGroup)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.tickle_edit_section_contact),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = contactSearch,
                            onValueChange = { contactSearch = it },
                            label = { Text(stringResource(R.string.tickle_edit_search_contacts)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!groupBound && selectedContact != null) {
                        item {
                            SelectedContactChip(
                                contact = selectedContact!!,
                                onClear = { selectedContact = null }
                            )
                        }
                    }
                    // Hide the list once a contact is picked. For a NEW tickle, the
                    // list reappears when the user clears the chip; for an EDIT it
                    // only appears while the user is typing in the search field.
                    // Group-bound tickles never show the contact list.
                    val showContactList = !groupBound && (contactSearch.isNotBlank() ||
                        (tickleId == null && selectedContact == null))
                    if (showContactList) {
                        items(filteredContacts.take(8)) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedContact = contact
                                        contactSearch = ""
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WarmCategory.Community.palette.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        contact.initials,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(contact.fullName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Frequency selector
                    item {
                        Text(
                            stringResource(R.string.tickle_edit_section_frequency),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showFrequencyDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    stringResource(selectedFrequency.displayNameResId),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showFrequencyDropdown,
                                onDismissRequest = { showFrequencyDropdown = false }
                            ) {
                                TickleFrequency.entries.forEach { freq ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(freq.displayNameResId)) },
                                        onClick = {
                                            selectedFrequency = freq
                                            showFrequencyDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFrequency == TickleFrequency.ONE_TIME || selectedFrequency == TickleFrequency.ANNUAL) {
                        item {
                            Text(
                                stringResource(R.string.tickle_edit_section_date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showStartDatePicker() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(formattedStartDate, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.common_change))
                            }
                        }
                    }

                    // Custom interval stepper
                    if (selectedFrequency == TickleFrequency.CUSTOM) {
                        item {
                            Text(
                                stringResource(R.string.tickle_edit_section_custom_interval),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (customIntervalDays > 1) customIntervalDays-- },
                                    shape = CircleShape
                                ) {
                                    Text("−")
                                }
                                Text(
                                    stringResource(R.string.tickle_edit_custom_interval_days, customIntervalDays),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(
                                    onClick = { customIntervalDays++ },
                                    shape = CircleShape
                                ) {
                                    Text("+")
                                }
                            }
                        }
                    }

                    // Locale-neutral annual presets
                    item {
                        Text(
                            stringResource(R.string.tickle_edit_section_common_annual_events),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnnualPresetChip(
                                label = stringResource(R.string.tickle_edit_preset_birthday),
                                onClick = {
                                    selectedFrequency = TickleFrequency.ANNUAL
                                    note = context.getString(R.string.tickle_edit_preset_note_birthday)
                                }
                            )
                            AnnualPresetChip(
                                label = stringResource(R.string.tickle_edit_preset_anniversary),
                                onClick = {
                                    selectedFrequency = TickleFrequency.ANNUAL
                                    note = context.getString(R.string.tickle_edit_preset_note_anniversary)
                                }
                            )
                            AnnualPresetChip(
                                label = stringResource(R.string.tickle_edit_preset_special_event),
                                onClick = {
                                    selectedFrequency = TickleFrequency.ANNUAL
                                    note = context.getString(R.string.tickle_edit_preset_note_special_event)
                                }
                            )
                        }
                    }

                    // Note
                    item {
                        Text(
                            stringResource(R.string.tickle_edit_section_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(stringResource(R.string.tickle_edit_note_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualPresetChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) }
    )
}

/** TIC-88: read-only header for a group-bound tickle — the group can't be
 *  changed from the edit form, so this shows the emoji + name without a clear
 *  affordance. Renders a placeholder puck while the group is still resolving. */
@Composable
private fun BoundGroupChip(group: ContactGroup?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(WarmCategory.Community.palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                group?.emoji ?: "👥",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            group?.name ?: "…",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SelectedContactChip(contact: Contact, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(WarmCategory.Community.palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.initials,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(contact.fullName, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onClear, contentPadding = PaddingValues(0.dp)) {
            Text("×", style = MaterialTheme.typography.titleMedium)
        }
    }
}
