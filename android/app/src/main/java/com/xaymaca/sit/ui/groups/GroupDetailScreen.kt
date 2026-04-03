package com.xaymaca.sit.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onContactClick: (Long) -> Unit,
    viewModel: GroupViewModel = hiltViewModel()
) {
    var group by remember { mutableStateOf<ContactGroup?>(null) }
    var members by remember { mutableStateOf(listOf<Contact>()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val allContacts by viewModel.allContacts.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(groupId) {
        val gwc = viewModel.getGroupWithContacts(groupId)
        group = gwc?.group
        members = gwc?.contacts ?: emptyList()
    }

    // Refresh members when viewModel state changes
    val groupsWithContacts by viewModel.groupsWithContacts.collectAsState()
    LaunchedEffect(groupsWithContacts) {
        val gwc = groupsWithContacts.find { it.group.id == groupId }
        if (gwc != null) {
            group = gwc.group
            members = gwc.contacts
        }
    }

    val nonMembers = remember(allContacts, members) {
        val memberIds = members.map { it.id }.toSet()
        allContacts.filter { it.id !in memberIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val g = group
                        if (g != null) {
                            Text("${g.emoji} ${g.name}", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit group")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Cobalt,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add members")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (members.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No members yet. Tap + to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(members, key = { it.id }) { contact ->
                        SwipeToRemoveMemberRow(
                            contact = contact,
                            onClick = { onContactClick(contact.id) },
                            onRemove = {
                                viewModel.removeMember(contact.id, groupId)
                            }
                        )
                        HorizontalDivider(
                            color = NavyLight,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }

        val currentGroup = group
        if (showEditDialog && currentGroup != null) {
            EditGroupDialog(
                group = currentGroup,
                onDismiss = { showEditDialog = false },
                onSave = { name, emoji ->
                    viewModel.updateGroup(currentGroup.copy(name = name, emoji = emoji))
                    showEditDialog = false
                }
            )
        }

        if (showAddSheet) {
            AddMembersBottomSheet(
                candidates = nonMembers,
                toastMessage = toastMessage,
                onAdd = { contact ->
                    viewModel.addMember(contact.id, groupId)
                    val groupName = group?.name ?: ""
                    val displayName = if (groupName.length <= 20) groupName else "group"
                    viewModel.showToast("${contact.fullName} added to $displayName")
                },
                onDismiss = { showAddSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemoveMemberRow(
    contact: Contact,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Cobalt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = contact.fullName.ifBlank { "No Name" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (contact.company.isNotBlank()) {
                    Text(
                        text = contact.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMembersBottomSheet(
    candidates: List<Contact>,
    toastMessage: String?,
    onAdd: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
            Column {
                Text(
                    text = "Add Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (candidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "All contacts are already in this group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 48.dp)
                    ) {
                        items(candidates, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(contact) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Cobalt),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.initials,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.fullName.ifBlank { "No Name" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (contact.company.isNotBlank()) {
                                        Text(
                                            text = contact.company,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { onAdd(contact) }) {
                                    Text("Add", color = Cobalt)
                                }
                            }
                        }
                    }
                }
            }

            // Toast overlay — inside the sheet's own window layer so it paints on top
            GroupToast(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun EditGroupDialog(
    group: ContactGroup,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var emoji by remember { mutableStateOf(group.emoji) }
    val canSave = name.trim().isNotEmpty() && name.length <= 30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${name.length} / 30",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (name.length >= 30)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), emoji.trim().ifBlank { "👥" }) },
                enabled = canSave
            ) {
                Text("Save", color = Cobalt)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GroupToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        message?.let {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cobalt
            ) {
                Text(
                    text = it,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
