package com.xaymaca.sit.ui.compose

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.service.SmsService
import com.xaymaca.sit.ui.shared.TicklrToast
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val selectedIds by viewModel.selectedContactIds.collectAsState()
    val messageBody by viewModel.messageBody.collectAsState()
    val sendDirectly by viewModel.sendDirectly.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var showTemplateDropdown by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var templateTitleInput by remember { mutableStateOf("") }

    // Contacts with at least one phone number among the selected set — used for send dispatch only
    val validSelectedContacts = remember(contacts, selectedIds) {
        contacts.filter { contact ->
            contact.id in selectedIds && parseJsonStringArray(contact.phoneNumbers).isNotEmpty()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Template picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showTemplateDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Templates",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showTemplateDropdown,
                        onDismissRequest = { showTemplateDropdown = false }
                    ) {
                        if (templates.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No templates saved") },
                                onClick = { showTemplateDropdown = false },
                                enabled = false
                            )
                        } else {
                            templates.forEach { template ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(template.title, fontWeight = FontWeight.Medium)
                                            Text(
                                                template.body.take(60) + if (template.body.length > 60) "…" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.applyTemplate(template)
                                        showTemplateDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showSaveTemplateDialog = true }) {
                    Text("Save", color = Cobalt)
                }
            }

            // Message editor
            OutlinedTextField(
                value = messageBody,
                onValueChange = viewModel::setMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp)
                    .padding(horizontal = 16.dp),
                label = { Text("Message") },
                placeholder = { Text("Type your message…") },
                shape = RoundedCornerShape(10.dp)
            )

            // Selected count + send button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedIds.size} recipient${if (selectedIds.size != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        if (messageBody.isBlank()) {
                            Toast.makeText(context, "Message is empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (validSelectedContacts.isEmpty()) {
                            Toast.makeText(context, "Select at least one contact", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val smsService = SmsService()
                        val hasSmsPermission = ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                        val phoneNumbers = validSelectedContacts.mapNotNull { contact ->
                            parseJsonStringArray(contact.phoneNumbers).firstOrNull()
                        }

                        if (sendDirectly && hasSmsPermission) {
                            val results = smsService.sendSmsToMany(context, phoneNumbers, messageBody)
                            val successCount = results.values.count { it }
                            Toast.makeText(
                                context,
                                "Sent to $successCount/${phoneNumbers.size} recipients",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearSelection()
                        } else {
                            val intent = smsService.sendSmsIntent(context, phoneNumbers, messageBody)
                            context.startActivity(intent)
                        }
                    },
                    enabled = validSelectedContacts.isNotEmpty() && messageBody.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send")
                }
            }

            HorizontalDivider(color = NavyLight)

            // Search + sort bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.weight(1f),
                    label = { Text("Search contacts") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showSortDropdown = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                        listOf(
                            ComposeViewModel.SortOrder.AZ     to "A – Z",
                            ComposeViewModel.SortOrder.ZA     to "Z – A",
                            ComposeViewModel.SortOrder.RECENT to "Recently Added"
                        ).forEach { (order, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Contact list with checkboxes
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCheckRow(
                        contact = contact,
                        isSelected = contact.id in selectedIds,
                        onToggle = { viewModel.toggleContactSelection(contact.id) }
                    )
                }
            }
        }
    }

        TicklrToast(
            message = toastMessage,
            onDismiss = viewModel::clearToast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    } // end Box

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save Template") },
            text = {
                Column {
                    OutlinedTextField(
                        value = templateTitleInput,
                        onValueChange = { templateTitleInput = it },
                        label = { Text("Template Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (messageBody.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${messageBody.take(80)}…\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateTitleInput.isNotBlank() && messageBody.isNotBlank()) {
                            viewModel.saveTemplate(templateTitleInput, messageBody)
                            templateTitleInput = ""
                            showSaveTemplateDialog = false
                        }
                    },
                    enabled = templateTitleInput.isNotBlank() && messageBody.isNotBlank()
                ) {
                    Text("Save", color = Cobalt)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel") }
            }
        )
    }
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

@Composable
private fun ContactCheckRow(
    contact: Contact,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null, // Row.clickable handles toggle; null prevents double-fire
            colors = CheckboxDefaults.colors(checkedColor = Cobalt)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) Cobalt else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.initials,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
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
    }
}
