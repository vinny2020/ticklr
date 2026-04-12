package com.xaymaca.sit.ui.network

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.ui.groups.GroupViewModel
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    onAddTickle: () -> Unit,
    onEdit: () -> Unit,
    onCompose: (Long) -> Unit = {},
    viewModel: NetworkViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var contact by remember { mutableStateOf<Contact?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGroupSheet by remember { mutableStateOf(false) }

    val allGroups by groupViewModel.groups.collectAsState()
    val contactGroups by groupViewModel.getGroupsForContact(contactId)
        .collectAsState(initial = emptyList())
    val contactGroupIds = remember(contactGroups) { contactGroups.map { it.id }.toSet() }

    LaunchedEffect(contactId) {
        contact = viewModel.getContactById(contactId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.fullName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit))
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
        val c = contact
        if (c == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Cobalt)
            }
        } else {
            val phoneNumbers = parseJsonStringArray(c.phoneNumbers)
            val emails = parseJsonStringArray(c.emails)
            val tags = parseJsonStringArray(c.tags)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Avatar + name header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Cobalt),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = c.initials,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = c.fullName.ifBlank { stringResource(R.string.common_no_name) },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (c.jobTitle.isNotBlank() || c.company.isNotBlank()) {
                            val subtitle = listOf(c.jobTitle, c.company)
                                .filter { it.isNotBlank() }
                                .joinToString(" at ")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Add Tickle + Add to Group buttons (row)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddTickle,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Amber,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.contact_detail_add_tickle), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { showGroupSheet = true },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.contact_detail_add_to_group), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Message button (full width)
                item {
                    Button(
                        onClick = { onCompose(contactId) },
                        enabled = phoneNumbers.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.contact_detail_message), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Phone numbers
                if (phoneNumbers.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.contact_detail_section_phone)) }
                    items(phoneNumbers) { number ->
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Cobalt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Emails
                if (emails.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.contact_detail_section_email)) }
                    items(emails) { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Cobalt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Notes
                if (c.notes.isNotBlank()) {
                    item {
                        SectionHeader(stringResource(R.string.contact_detail_section_notes))
                        Text(
                            text = c.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tags
                if (tags.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.contact_detail_section_tags))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }

                // Delete button
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(stringResource(R.string.contact_detail_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Add to Group bottom sheet
    if (showGroupSheet) {
        val c = contact
        if (c != null) {
            AddToGroupSheet(
                contactId = contactId,
                allGroups = allGroups,
                memberGroupIds = contactGroupIds,
                onToggle = { groupId, isMember ->
                    if (isMember) groupViewModel.removeMember(contactId, groupId)
                    else groupViewModel.addMember(contactId, groupId)
                },
                groupViewModel = groupViewModel,
                onDismiss = { showGroupSheet = false }
            )
        }
    }

    if (showDeleteDialog) {
        val c = contact
        if (c != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.network_delete_title)) },
                text = { Text(stringResource(R.string.network_delete_message, c.fullName)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteContact(c)
                            showDeleteDialog = false
                            onBack()
                        }
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToGroupSheet(
    contactId: Long,
    allGroups: List<com.xaymaca.sit.data.model.ContactGroup>,
    memberGroupIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    groupViewModel: GroupViewModel,
    onDismiss: () -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.contact_add_to_group_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (allGroups.isEmpty() && !showCreateField) {
                Text(
                    stringResource(R.string.contact_add_to_group_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            allGroups.forEach { group ->
                val isMember = group.id in memberGroupIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(group.id, isMember) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${group.emoji} ${group.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (isMember) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.contact_group_member_check),
                            tint = Cobalt,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Create new group row
            if (!showCreateField) {
                TextButton(
                    onClick = { showCreateField = true },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.contact_add_to_group_create_new), color = Cobalt)
                }
            } else {
                val isDuplicateInline = newGroupName.trim().isNotBlank() &&
                    groupViewModel.isGroupNameTaken(newGroupName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { if (it.length <= 30) newGroupName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.contact_add_to_group_group_name)) },
                        singleLine = true,
                        isError = isDuplicateInline,
                        shape = RoundedCornerShape(10.dp),
                        supportingText = {
                            if (isDuplicateInline) {
                                Text(
                                    stringResource(R.string.group_dialog_name_exists),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(stringResource(R.string.group_dialog_char_count, newGroupName.length))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank() && !isDuplicateInline) {
                                groupViewModel.createGroupAndAddContact(newGroupName, contactId)
                                newGroupName = ""
                                showCreateField = false
                            }
                        },
                        enabled = newGroupName.isNotBlank() && !isDuplicateInline,
                        colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.common_create))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 16.dp)
    )
}

private fun parseJsonStringArray(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
    return trimmed
        .removePrefix("[")
        .removeSuffix("]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
}
