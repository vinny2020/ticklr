package com.xaymaca.sit.ui.tickle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.ui.network.NetworkViewModel
import com.xaymaca.sit.ui.shared.TicklrToast
import com.xaymaca.sit.ui.shared.displayNameResId
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleEditScreen(
    tickleId: Long?,
    preselectedContactId: Long? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    tickleViewModel: TickleViewModel = hiltViewModel(),
    networkViewModel: NetworkViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val contacts by networkViewModel.filteredContacts.collectAsState()
    val toastMessage by tickleViewModel.toastMessage.collectAsState()

    // Form state — contact only. Group-based tickles were removed; legacy ones
    // load with selectedContact == null so the user picks a contact before saving.
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var contactSearch by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(TickleFrequency.MONTHLY) }
    var customIntervalDays by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(tickleId == null) }

    // Pre-populate contact when navigating from ContactDetail
    LaunchedEffect(preselectedContactId) {
        if (tickleId == null && preselectedContactId != null) {
            selectedContact = networkViewModel.getContactById(preselectedContactId)
        }
    }

    // Load existing reminder for edit mode. Legacy group-based tickles are
    // auto-converted: their frequency / note / startDate carry over, but the
    // group association is dropped and selectedContact stays null until the
    // user picks one (Save remains disabled until they do).
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
                if (reminder.contactId != null) {
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                        val canSave = isLoaded && selectedContact != null
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val nextDue = TickleScheduler.nextDueDate(
                                        from = startDate,
                                        frequency = selectedFrequency.name,
                                        customDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null
                                    )
                                    val reminder = TickleReminder(
                                        id = tickleId ?: 0L,
                                        contactId = selectedContact?.id,
                                        groupId = null,
                                        note = note.trim(),
                                        frequency = selectedFrequency.name,
                                        customIntervalDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null,
                                        startDate = startDate,
                                        nextDueDate = nextDue
                                    )
                                    tickleViewModel.upsert(reminder, isNew = tickleId == null)
                                    delay(2000)
                                    onSaved()
                                }
                            },
                            enabled = canSave
                        ) {
                            Text(
                                stringResource(R.string.common_save),
                                color = if (canSave) Amber else Amber.copy(alpha = 0.38f),
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
                    CircularProgressIndicator(color = Cobalt)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Pinned contact-search header. Keeping the search field outside
                    // the LazyColumn ensures it (and the result rows below it) stay
                    // visible when the keyboard is up.
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

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .imePadding(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedContact != null) {
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
                        val showContactList = contactSearch.isNotBlank() ||
                            (tickleId == null && selectedContact == null)
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
                                            .background(Cobalt),
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

        TicklrToast(
            message = toastMessage,
            onDismiss = tickleViewModel::clearToast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
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
                .background(Cobalt),
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
