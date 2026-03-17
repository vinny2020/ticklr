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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.service.TickleScheduler
import com.xaymaca.sit.ui.groups.GroupViewModel
import com.xaymaca.sit.ui.network.NetworkViewModel
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleEditScreen(
    tickleId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    tickleViewModel: TickleViewModel = hiltViewModel(),
    networkViewModel: NetworkViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val contacts by networkViewModel.filteredContacts.collectAsState()
    val groups by groupViewModel.groups.collectAsState()

    // Form state
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Contact, 1 = Group
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedGroup by remember { mutableStateOf<ContactGroup?>(null) }
    var contactSearch by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(TickleFrequency.MONTHLY) }
    var customIntervalDays by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(tickleId == null) }

    // Load existing reminder for edit mode
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
                    selectedTab = 0
                    selectedContact = networkViewModel.getContactById(reminder.contactId)
                } else if (reminder.groupId != null) {
                    selectedTab = 1
                    selectedGroup = groupViewModel.getGroupById(reminder.groupId)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (tickleId == null) "New Tickle" else "Edit Tickle",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                                    contactId = if (selectedTab == 0) selectedContact?.id else null,
                                    groupId = if (selectedTab == 1) selectedGroup?.id else null,
                                    note = note.trim(),
                                    frequency = selectedFrequency.name,
                                    customIntervalDays = if (selectedFrequency == TickleFrequency.CUSTOM) customIntervalDays else null,
                                    startDate = startDate,
                                    nextDueDate = nextDue
                                )
                                tickleViewModel.upsert(reminder)
                                onSaved()
                            }
                        },
                        enabled = isLoaded && (selectedContact != null || selectedGroup != null)
                    ) {
                        Text("Save", color = Amber, fontWeight = FontWeight.SemiBold)
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
                // Tab row: Contact vs Group
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Cobalt
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Contact") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Group") }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact or Group picker
                    if (selectedTab == 0) {
                        item {
                            Text(
                                "CONTACT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contactSearch,
                                onValueChange = { contactSearch = it },
                                label = { Text("Search contacts") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (selectedContact != null) {
                            item {
                                SelectedContactChip(
                                    contact = selectedContact!!,
                                    onClear = { selectedContact = null }
                                )
                            }
                        }
                        if (selectedContact == null) {
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
                    } else {
                        item {
                            Text(
                                "GROUP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (selectedGroup != null) {
                            item {
                                AssistChip(
                                    onClick = { selectedGroup = null },
                                    label = { Text("${selectedGroup!!.emoji} ${selectedGroup!!.name}") },
                                    trailingIcon = null
                                )
                            }
                        } else {
                            items(groups) { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGroup = group }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.emoji,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(group.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }

                    // Frequency selector
                    item {
                        Text(
                            "FREQUENCY",
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
                                    selectedFrequency.label,
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
                                        text = { Text(freq.label) },
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
                                "CUSTOM INTERVAL (DAYS)",
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
                                    "$customIntervalDays days",
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
                            "NOTE (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("What to talk about…") },
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
