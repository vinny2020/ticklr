package com.xaymaca.sit.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.ContactGroup
import com.xaymaca.sit.data.model.GroupWithContacts
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmRadius
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.warm.WarmCard
import com.xaymaca.sit.ui.warm.WarmCardVariant
import com.xaymaca.sit.ui.warm.WarmEyebrow
import com.xaymaca.sit.ui.warm.WarmListContainer
import com.xaymaca.sit.ui.warm.WarmRowDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onGroupClick: (Long) -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val warmth = Warmth.Subtle
    val palette = WarmTheme.palette(warmth)
    val groupsWithContacts by viewModel.groupsWithContacts.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<ContactGroup?>(null) }

    val canonicalGroups = groupsWithContacts
        .filter { it.group.categoryId != null }
        .sortedBy { WarmCategory.from(it.group.categoryId)?.sortOrder ?: Int.MAX_VALUE }
    val userGroups = groupsWithContacts
        .filter { it.group.categoryId == null }
        .sortedBy { it.group.createdAt }

    Scaffold(
        containerColor = palette.paper,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = WarmCategory.Community.palette.accent,
                contentColor = palette.paper,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.group_list_create_fab))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                WarmHeader(palette = palette, warmth = warmth)
            }

            items(canonicalGroups, key = { it.group.id }) { gwc ->
                val category = WarmCategory.from(gwc.group.categoryId) ?: return@items
                WarmCard(
                    category = category,
                    variant = WarmCardVariant.Row,
                    warmth = warmth,
                    showPrompt = true,
                    contactsCount = gwc.contacts.size,
                    onClick = { onGroupClick(gwc.group.id) },
                    modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
                )
            }

            if (userGroups.isNotEmpty()) {
                item {
                    WarmEyebrow(
                        text = stringResource(R.string.warm_groups_yourGroups),
                        warmth = warmth,
                        modifier = Modifier.padding(top = 12.dp, start = WarmSpacing.Lg, end = WarmSpacing.Lg),
                    )
                }
                item {
                    WarmListContainer(
                        warmth = warmth,
                        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
                    ) {
                        userGroups.forEachIndexed { idx, gwc ->
                            UserGroupRow(
                                groupWithContacts = gwc,
                                onClick = { onGroupClick(gwc.group.id) },
                                onDelete = { groupToDelete = gwc.group },
                                palette = palette,
                            )
                            if (idx < userGroups.size - 1) WarmRowDivider(warmth = warmth)
                        }
                    }
                }
            }

            if (canonicalGroups.isEmpty() && userGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.group_list_empty_title),
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.ink2),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.group_list_empty_description),
                                style = TextStyle(fontSize = 13.sp, color = palette.ink3),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, emoji ->
                viewModel.createGroup(name, emoji)
                showCreateDialog = false
            },
            isNameTaken = { viewModel.isGroupNameTaken(it) },
        )
    }

    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(stringResource(R.string.group_delete_title)) },
            text = { Text(stringResource(R.string.group_delete_message, group.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group)
                        groupToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun WarmHeader(palette: com.xaymaca.sit.ui.theme.WarmPalette, warmth: Warmth) {
    Column(
        modifier = Modifier.padding(start = WarmSpacing.Lg, end = WarmSpacing.Lg, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.group_list_title),
            style = WarmHeadingFont.style(32.sp, warmth).copy(color = palette.ink),
        )
        Text(
            text = stringResource(R.string.warm_groups_subtitle),
            style = TextStyle(fontSize = 14.sp, color = palette.ink2),
        )
    }
}

@Composable
private fun UserGroupRow(
    groupWithContacts: GroupWithContacts,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    palette: com.xaymaca.sit.ui.theme.WarmPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = WarmSpacing.Lg, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.paperSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = groupWithContacts.group.emoji,
                style = TextStyle(fontSize = 28.sp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = groupWithContacts.group.name,
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                maxLines = 1,
            )
            val count = groupWithContacts.contacts.size
            Text(
                text = pluralStringResource(R.plurals.group_list_member_count, count, count),
                style = TextStyle(fontSize = 13.sp, color = palette.ink2),
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = palette.ink3,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    isNameTaken: suspend (String) -> Boolean,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("👥") }
    var isDuplicate by remember { mutableStateOf(false) }

    LaunchedEffect(name) {
        isDuplicate = name.trim().isNotBlank() && isNameTaken(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_new_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text(stringResource(R.string.group_dialog_emoji_label)) },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text(stringResource(R.string.group_dialog_name_label)) },
                    singleLine = true,
                    isError = isDuplicate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (isDuplicate) {
                                Text(
                                    text = stringResource(R.string.group_dialog_name_exists),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text(
                                text = stringResource(R.string.group_dialog_char_count, name.length),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (name.length >= 30) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && !isDuplicate) onCreate(name, emoji) },
                enabled = name.isNotBlank() && !isDuplicate,
            ) {
                Text(stringResource(R.string.common_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
