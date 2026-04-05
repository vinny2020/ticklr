package com.xaymaca.sit.ui.compose

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.service.SmsService
import com.xaymaca.sit.ui.shared.TicklrToast
import com.xaymaca.sit.ui.theme.Cobalt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onNavigateToNetwork: () -> Unit = {},
    initialContactId: Long? = null,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    LaunchedEffect(initialContactId) {
        if (initialContactId != null) viewModel.preSelectContact(initialContactId)
    }
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val messageBody by viewModel.messageBody.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val canSend by viewModel.canSend.collectAsState()

    var showTemplateDropdown by remember { mutableStateOf(false) }
    var showContactDropdown by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf<String?>(null) }
    val messageFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose", fontWeight = FontWeight.Bold) },
                    actions = {
                        if (selectedContact != null || messageBody.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.clearCompose()
                                selectedTemplateName = null
                                onNavigateToNetwork()
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear compose",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // To: contact search field
                Box {
                    OutlinedTextField(
                        value = if (selectedContact != null) "" else searchQuery,
                        onValueChange = { query ->
                            if (selectedContact == null) {
                                viewModel.setSearchQuery(query)
                                showContactDropdown = query.isNotBlank()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("To") },
                        placeholder = { Text("Search contacts…") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedContact == null
                    )
                    DropdownMenu(
                        expanded = showContactDropdown && contacts.isNotEmpty() && selectedContact == null,
                        onDismissRequest = { showContactDropdown = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        contacts.take(8).forEach { contact ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(contact.fullName.ifBlank { "No Name" }, fontWeight = FontWeight.Medium)
                                        if (contact.company.isNotBlank()) {
                                            Text(
                                                contact.company,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectContact(contact)
                                    showContactDropdown = false
                                    selectedTemplateName = null
                                }
                            )
                        }
                    }
                }

                // Selected contact chip
                if (selectedContact != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text(selectedContact!!.fullName.ifBlank { "No Name" }) },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.clearContact() },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove contact",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Cobalt.copy(alpha = 0.15f),
                            selectedLabelColor = Cobalt
                        )
                    )
                    // No phone warning
                    val phones = parseJsonStringArray(selectedContact!!.phoneNumbers)
                    if (phones.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "No phone number on file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Template dropdown — hidden if no templates
                if (templates.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { showTemplateDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedTemplateName ?: "Select template",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showTemplateDropdown,
                            onDismissRequest = { showTemplateDropdown = false }
                        ) {
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
                                        selectedTemplateName = template.title
                                        showTemplateDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Message body
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = viewModel::setMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .focusRequester(messageFocusRequester),
                    label = { Text("Message") },
                    placeholder = { Text("Type your message…") },
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Send button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val contact = selectedContact ?: return@Button
                            val phones = parseJsonStringArray(contact.phoneNumbers)
                            val phone = phones.firstOrNull() ?: return@Button
                            val smsService = SmsService()
                            val hasSmsPermission = ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            // Read pref fresh at send time — not from a stale ViewModel StateFlow
                            val sendDirectly = context
                                .getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
                                .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)

                            if (sendDirectly && hasSmsPermission) {
                                smsService.sendSms(context, phone, messageBody)
                            } else {
                                val intent = smsService.sendSmsIntent(context, listOf(phone), messageBody)
                                context.startActivity(intent)
                            }
                            viewModel.clearCompose()
                            selectedTemplateName = null
                            viewModel.showToast("Message sent \u2713")
                        },
                        enabled = canSend,
                        colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send")
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
