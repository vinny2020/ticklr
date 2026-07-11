package com.xaymaca.sit.ui.network

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.ui.shared.PendingPhoneChoice
import com.xaymaca.sit.ui.shared.PhoneChoice
import com.xaymaca.sit.ui.shared.PhoneChooser
import com.xaymaca.sit.ui.shared.PhoneNumberChooserDialog
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.warm.ContactPhotoView
import com.xaymaca.sit.ui.warm.ContactPhotoStyle
import com.xaymaca.sit.ui.warm.FilterChipKind
import com.xaymaca.sit.ui.warm.WarmFilterChip
import kotlinx.coroutines.launch

private val CHIP_CATEGORIES = listOf(
    WarmCategory.Family,
    WarmCategory.Friends,
    WarmCategory.Work,
    WarmCategory.Community,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkListScreen(
    onContactClick: (Long) -> Unit,
    onAddContact: () -> Unit,
    onImport: () -> Unit,
    /** TIC-96: long-press "Send a text" quick action. */
    onCompose: (contactId: Long, reminderId: Long?) -> Unit = { _, _ -> },
    /** TIC-96: long-press "Create a tickle" quick action. */
    onAddTickleForContact: (Long) -> Unit = {},
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val warmth = Warmth.Subtle
    val palette = WarmTheme.palette(warmth)
    val contacts by viewModel.filteredContacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val groupFilter by viewModel.groupFilter.collectAsState()
    val userGroups by viewModel.userGroups.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // TIC-96: a resolved multi-number choice awaiting the user's pick for the
    // long-press quick action's Call row — see PhoneChooser.
    var phoneChoice by remember { mutableStateOf<PendingPhoneChoice?>(null) }

    Scaffold(
        containerColor = palette.paper,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContact,
                containerColor = WarmCategory.Community.palette.accent,
                contentColor = palette.paper,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.network_add_contact_fab))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        ) {
            item {
                Header(palette = palette, warmth = warmth, showMenu = showMenu,
                       onShowMenu = { showMenu = it },
                       onImport = { onImport() })
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WarmSpacing.Lg, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.network_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.cardBorder,
                        unfocusedBorderColor = palette.cardBorder,
                        focusedContainerColor = palette.cardBg,
                        unfocusedContainerColor = palette.cardBg,
                    ),
                )
            }

            item {
                FilterChipRow(
                    viewModel = viewModel,
                    activeCategoryId = categoryFilter,
                    activeGroupId = groupFilter,
                    userGroups = userGroups,
                    warmth = warmth,
                )
            }

            items(contacts, key = { it.id }) { contact ->
                ContactRow(
                    contact = contact,
                    palette = palette,
                    onClick = { onContactClick(contact.id) },
                    // TIC-96: long-press quick actions — Send a text / Create a
                    // tickle / Call / Delete — replacing the delete-only long
                    // press. Delete keeps its existing confirm dialog below.
                    onSendText = {
                        coroutineScope.launch {
                            val dueReminderId = viewModel.dueReminderIdForContact(contact.id)
                            onCompose(contact.id, dueReminderId)
                        }
                    },
                    onCreateTickle = { onAddTickleForContact(contact.id) },
                    onCall = {
                        val phones = stringListConverter.fromString(contact.phoneNumbers)
                        when (val choice = PhoneChooser.choose(phones)) {
                            is PhoneChoice.Direct ->
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${choice.number}")))
                            is PhoneChoice.NeedsChoice ->
                                phoneChoice = PendingPhoneChoice(choice.numbers) { number ->
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                                }
                            PhoneChoice.None -> {}
                        }
                    },
                    onDeleteRequested = { contactToDelete = contact },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp)
                        .height(1.dp)
                        .background(palette.cardBorder),
                )
            }

            if (contacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val msgRes = if (categoryFilter != null || groupFilter != null)
                                         R.string.warm_network_empty_filtered
                                     else R.string.network_empty_title
                        Text(
                            text = stringResource(msgRes),
                            style = TextStyle(fontSize = 15.sp, color = palette.ink2),
                        )
                    }
                }
            }
        }
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text(stringResource(R.string.network_delete_title)) },
            text = { Text(stringResource(R.string.network_delete_message, contact.fullName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContact(contact)
                        contactToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    phoneChoice?.let { pending ->
        PhoneNumberChooserDialog(
            numbers = pending.numbers,
            onSelect = { number -> pending.onChosen(number); phoneChoice = null },
            onDismiss = { phoneChoice = null },
        )
    }
}

@Composable
private fun Header(
    palette: com.xaymaca.sit.ui.theme.WarmPalette,
    warmth: Warmth,
    showMenu: Boolean,
    onShowMenu: (Boolean) -> Unit,
    onImport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.network_title),
                style = WarmHeadingFont.style(32.sp, warmth).copy(color = palette.ink),
            )
            Text(
                text = stringResource(R.string.warm_network_subtitle),
                style = TextStyle(fontSize = 14.sp, color = palette.ink2),
            )
        }
        Box {
            IconButton(onClick = { onShowMenu(true) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.network_more_options),
                    tint = palette.ink,
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenu(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.network_import_contacts)) },
                    onClick = { onShowMenu(false); onImport() },
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    viewModel: NetworkViewModel,
    activeCategoryId: String?,
    activeGroupId: Long?,
    userGroups: List<com.xaymaca.sit.data.model.ContactGroup>,
    warmth: Warmth,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = WarmSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            WarmFilterChip(
                kind = FilterChipKind.All,
                label = stringResource(R.string.warm_network_filter_all),
                // "All" is active only when no category AND no group filter is set.
                isActive = activeCategoryId == null && activeGroupId == null,
                onClick = { viewModel.setCategoryFilter(null); viewModel.setGroupFilter(null) },
                warmth = warmth,
            )
        }
        items(CHIP_CATEGORIES) { cat ->
            val count by viewModel.countFor(cat.id).collectAsState()
            if (count > 0 || activeCategoryId == cat.id) {
                WarmFilterChip(
                    kind = FilterChipKind.CategoryKind(cat),
                    label = stringResource(cat.labelRes),
                    count = count,
                    isActive = activeCategoryId == cat.id,
                    onClick = { viewModel.setCategoryFilter(cat.id) },
                    warmth = warmth,
                )
            }
        }
        // TIC-88: user-created groups follow the canonical categories, each a
        // neutral pill that filters the list to that group's members.
        items(userGroups, key = { it.id }) { group ->
            WarmFilterChip(
                kind = FilterChipKind.GroupKind,
                label = group.name,
                isActive = activeGroupId == group.id,
                onClick = { viewModel.setGroupFilter(group.id) },
                warmth = warmth,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(
    contact: Contact,
    palette: com.xaymaca.sit.ui.theme.WarmPalette,
    onClick: () -> Unit,
    /** TIC-96: long-press quick actions, replacing the previous delete-only
     *  long press. Delete keeps its confirm dialog (owned by the caller). */
    onSendText: () -> Unit,
    onCreateTickle: () -> Unit,
    onCall: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    // For row tinting we'd need to resolve the contact's first canonical
    // group; ContactPhotoView's fallback uses .Community when it can't
    // resolve, which is the spec'd behavior for uncategorized contacts.
    val category = WarmCategory.Community
    var showQuickActions by remember { mutableStateOf(false) }
    val hasPhone = remember(contact.phoneNumbers) {
        stringListConverter.fromString(contact.phoneNumbers).isNotEmpty()
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showQuickActions = true })
                .padding(horizontal = WarmSpacing.Lg, vertical = WarmSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactPhotoView(
                contact = contact,
                category = category,
                style = ContactPhotoStyle.List,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.fullName.ifBlank { stringResource(R.string.common_no_name) },
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                    maxLines = 1,
                )
                if (contact.company.isNotBlank()) {
                    Text(
                        text = contact.company,
                        style = TextStyle(fontSize = 13.sp, color = palette.ink2),
                        maxLines = 1,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = palette.ink3,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = showQuickActions,
            onDismissRequest = { showQuickActions = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.warm_contact_sendTickle)) },
                enabled = hasPhone,
                onClick = { showQuickActions = false; onSendText() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.warm_contact_createTickle)) },
                onClick = { showQuickActions = false; onCreateTickle() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.warm_contact_call)) },
                enabled = hasPhone,
                onClick = { showQuickActions = false; onCall() },
            )
            HorizontalDivider(color = palette.cardBorder)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { showQuickActions = false; onDeleteRequested() },
            )
        }
    }
}

// Single source of truth for JSON-array parsing — the same converter Room uses
// for the phoneNumbers column.
private val stringListConverter = StringListConverter()
