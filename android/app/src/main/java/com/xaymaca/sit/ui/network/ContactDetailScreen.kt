package com.xaymaca.sit.ui.network

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.service.ContactPhotoService
import com.xaymaca.sit.service.LocalPhotoStore
import com.xaymaca.sit.ui.groups.GroupViewModel
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmCategoryPalette
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmPalette
import com.xaymaca.sit.ui.theme.WarmRadius
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.warm.ContactPhotoStyle
import com.xaymaca.sit.ui.warm.ContactPhotoView
import com.xaymaca.sit.ui.warm.ContactsAccessBanner
import com.xaymaca.sit.ui.warm.WarmCard
import com.xaymaca.sit.ui.warm.WarmCardVariant
import com.xaymaca.sit.ui.warm.WarmEyebrow
import com.xaymaca.sit.ui.warm.WarmListContainer
import com.xaymaca.sit.ui.warm.WarmRowDivider
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    onAddTickle: () -> Unit,
    onEdit: () -> Unit,
    onCompose: (Long) -> Unit = {},
    viewModel: NetworkViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
) {
    val warmth = Warmth.Subtle
    val palette = WarmTheme.palette(warmth)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var contact by remember { mutableStateOf<Contact?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGroupSheet by remember { mutableStateOf(false) }
    var photoRefreshKey by remember { mutableStateOf(UUID.randomUUID()) }

    val allGroups by groupViewModel.groups.collectAsState()
    val contactGroups by groupViewModel.getGroupsForContact(contactId)
        .collectAsState(initial = emptyList())
    val contactGroupIds = remember(contactGroups) { contactGroups.map { it.id }.toSet() }

    // Resolve the warm category from the contact's first canonical group.
    val resolvedCategory: WarmCategory? = remember(contactGroups) {
        // Most-recent canonical: walk in reverse order of the membership list.
        contactGroups.reversed().firstNotNullOfOrNull { WarmCategory.from(it.categoryId) }
    }
    val categoryForTint: WarmCategory = resolvedCategory ?: WarmCategory.Community

    LaunchedEffect(contactId) {
        contact = viewModel.getContactById(contactId)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                LocalPhotoStore.write(context, contactId, uri)
                ContactPhotoService.clearCache()
                photoRefreshKey = UUID.randomUUID()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = palette.ink)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit), tint = palette.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.paper,
                    titleContentColor = palette.ink,
                ),
            )
        },
        containerColor = palette.paper,
    ) { paddingValues ->
        val c = contact
        if (c == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = categoryForTint.palette.accent) }
        } else {
            val phoneNumbers = parseJsonStringArray(c.phoneNumbers)
            val emails = parseJsonStringArray(c.emails)
            val tags = parseJsonStringArray(c.tags)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    PhotoHeader(
                        contact = c,
                        category = categoryForTint,
                        warmth = warmth,
                        palette = palette,
                        photoRefreshKey = photoRefreshKey,
                        onAddPhoto = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }

                item {
                    ContactsAccessBanner(
                        contact = c,
                        category = categoryForTint,
                        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
                    ) {
                        // Permission just landed — invalidate the
                        // system-Contacts cache and re-resolve.
                        ContactPhotoService.clearCache()
                        photoRefreshKey = UUID.randomUUID()
                    }
                }

                item {
                    ActionChipRow(
                        category = categoryForTint,
                        palette = palette,
                        canText = phoneNumbers.isNotEmpty(),
                        canCall = phoneNumbers.isNotEmpty(),
                        canEmail = emails.isNotEmpty(),
                        onSendText = { onCompose(contactId) },
                        onCreateTickle = onAddTickle,
                        onCall = {
                            phoneNumbers.firstOrNull()?.let { number ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                context.startActivity(intent)
                            }
                        },
                        onEmail = {
                            emails.firstOrNull()?.let { email ->
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                context.startActivity(intent)
                            }
                        },
                    )
                }

                if (phoneNumbers.isNotEmpty()) {
                    item {
                        DetailSection(title = stringResource(R.string.contact_detail_section_phone), warmth = warmth) {
                            phoneNumbers.forEach { number ->
                                LinkText(
                                    text = number,
                                    color = categoryForTint.palette.accent,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }
                }

                if (emails.isNotEmpty()) {
                    item {
                        DetailSection(title = stringResource(R.string.contact_detail_section_email), warmth = warmth) {
                            emails.forEach { email ->
                                LinkText(
                                    text = email,
                                    color = categoryForTint.palette.accent,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }
                }

                if (c.notes.isNotBlank()) {
                    item {
                        DetailSection(title = stringResource(R.string.contact_detail_section_notes), warmth = warmth) {
                            Text(
                                text = c.notes,
                                style = TextStyle(fontSize = 14.sp, color = palette.ink2),
                            )
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    item {
                        DetailSection(title = stringResource(R.string.contact_detail_section_tags), warmth = warmth) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                tags.forEach { tag ->
                                    SuggestionChip(onClick = {}, label = { Text(tag) })
                                }
                            }
                        }
                    }
                }

                item {
                    GroupsSection(
                        contactGroups = contactGroups,
                        palette = palette,
                        warmth = warmth,
                        category = categoryForTint,
                        onAddToGroup = { showGroupSheet = true },
                    )
                }

                if (resolvedCategory != null) {
                    item {
                        WarmCard(
                            category = resolvedCategory,
                            variant = WarmCardVariant.Hero,
                            warmth = warmth,
                            showPrompt = true,
                            modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg),
                    ) {
                        Text(
                            text = stringResource(R.string.contact_detail_delete),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

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
                onDismiss = { showGroupSheet = false },
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
                            LocalPhotoStore.delete(context, contactId)
                            viewModel.deleteContact(c)
                            showDeleteDialog = false
                            onBack()
                        },
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }
    }
}

// MARK: - Photo header

@Composable
private fun PhotoHeader(
    contact: Contact,
    category: WarmCategory,
    warmth: Warmth,
    palette: WarmPalette,
    photoRefreshKey: Any,
    onAddPhoto: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ContactPhotoView(
            contact = contact,
            category = category,
            style = ContactPhotoStyle.Detail,
            refreshKey = photoRefreshKey,
        )
        Row(
            modifier = Modifier.clickable(onClick = onAddPhoto),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = category.palette.accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.warm_contact_addPhoto),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = category.palette.accent),
            )
        }
        Column(
            modifier = Modifier.padding(top = 6.dp, start = WarmSpacing.Lg, end = WarmSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = contact.fullName.ifBlank { stringResource(R.string.common_no_name) },
                style = WarmHeadingFont.style(28.sp, warmth).copy(color = palette.ink),
            )
            if (contact.company.isNotBlank()) {
                Text(text = contact.company, style = TextStyle(fontSize = 14.sp, color = palette.ink2))
            }
            if (contact.jobTitle.isNotBlank()) {
                Text(text = contact.jobTitle, style = TextStyle(fontSize = 12.sp, color = palette.ink3))
            }
        }
    }
}

// MARK: - Action chip row

@Composable
private fun ActionChipRow(
    category: WarmCategory,
    palette: WarmPalette,
    canText: Boolean,
    canCall: Boolean,
    canEmail: Boolean,
    onSendText: () -> Unit,
    onCreateTickle: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionChip(
            title = stringResource(R.string.warm_contact_sendTickle),
            icon = Icons.Filled.Message,
            filled = true,
            enabled = canText,
            category = category,
            palette = palette,
            onClick = onSendText,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            title = stringResource(R.string.warm_contact_createTickle),
            icon = Icons.Filled.Notifications,
            filled = false,
            enabled = true,
            category = category,
            palette = palette,
            onClick = onCreateTickle,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            title = stringResource(R.string.warm_contact_call),
            icon = Icons.Filled.Phone,
            filled = false,
            enabled = canCall,
            category = category,
            palette = palette,
            onClick = onCall,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            title = stringResource(R.string.warm_contact_email),
            icon = Icons.Filled.Email,
            filled = false,
            enabled = canEmail,
            category = category,
            palette = palette,
            onClick = onEmail,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionChip(
    title: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    category: WarmCategory,
    palette: WarmPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cat = category.palette
    val background = if (filled) cat.accent else palette.cardBg
    val foreground = if (filled) Color(0xFFFAF4E2) else cat.accent
    val border = if (filled) Color.Transparent else palette.cardBorder
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = foreground),
            maxLines = 1,
        )
    }
}

// MARK: - Detail cards / sections

@Composable
private fun DetailSection(
    title: String,
    warmth: Warmth,
    content: @Composable () -> Unit,
) {
    val palette = WarmTheme.palette(warmth)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WarmEyebrow(text = title, warmth = warmth)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WarmRadius.Surface))
                .background(palette.cardBg)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(WarmRadius.Surface))
                .padding(WarmSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun LinkText(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        style = TextStyle(fontSize = 15.sp, color = color),
    )
}

@Composable
private fun GroupsSection(
    contactGroups: List<com.xaymaca.sit.data.model.ContactGroup>,
    palette: WarmPalette,
    warmth: Warmth,
    category: WarmCategory,
    onAddToGroup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WarmEyebrow(text = stringResource(R.string.group_list_title), warmth = warmth)
        if (contactGroups.isNotEmpty()) {
            WarmListContainer(warmth = warmth) {
                contactGroups.forEachIndexed { idx, group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WarmSpacing.Lg, vertical = WarmSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.paperSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = group.emoji, style = TextStyle(fontSize = 18.sp))
                        }
                        Text(
                            text = group.name,
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.ink),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (idx < contactGroups.size - 1) WarmRowDivider(warmth = warmth)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        // Always-visible "Add to a Group" outline pill — matches iOS
        // commit that surfaced the affordance even when contact has no
        // groups yet.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardBg)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onAddToGroup)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = null,
                tint = category.palette.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.contact_detail_add_to_group),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = category.palette.accent),
            )
        }
    }
}

// MARK: - Add to Group sheet (largely preserved, retinted)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToGroupSheet(
    contactId: Long,
    allGroups: List<com.xaymaca.sit.data.model.ContactGroup>,
    memberGroupIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    groupViewModel: GroupViewModel,
    onDismiss: () -> Unit,
) {
    val palette = WarmTheme.palette()
    var showCreateField by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.paper) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.contact_add_to_group_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (allGroups.isEmpty() && !showCreateField) {
                Text(
                    stringResource(R.string.contact_add_to_group_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink2,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            allGroups.forEach { group ->
                val isMember = group.id in memberGroupIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(group.id, isMember) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${group.emoji} ${group.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (isMember) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.contact_group_member_check),
                            tint = WarmCategory.Community.palette.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.cardBorder),
                )
            }

            if (!showCreateField) {
                TextButton(
                    onClick = { showCreateField = true },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.contact_add_to_group_create_new),
                        color = WarmCategory.Community.palette.accent,
                    )
                }
            } else {
                val isDuplicateInline = newGroupName.trim().isNotBlank() &&
                    groupViewModel.isGroupNameTaken(newGroupName)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text(stringResource(R.string.group_dialog_char_count, newGroupName.length))
                            }
                        },
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
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCategory.Community.palette.accent),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(stringResource(R.string.common_create))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun parseJsonStringArray(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    val trimmed = json.trim().removePrefix("[").removeSuffix("]")
    if (trimmed.isBlank()) return emptyList()
    return Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(trimmed)
        .map { it.groupValues[1].replace("\\\\\"", "\"").replace("\\\\\\\\", "\\\\") }
        .toList()
}
