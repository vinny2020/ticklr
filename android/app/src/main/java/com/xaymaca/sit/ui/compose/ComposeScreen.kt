package com.xaymaca.sit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.service.SmsService
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.ui.shared.TicklrToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    /** Called when the user is finished here — message handed off to the SMS
     *  app, or compose dismissed. The caller routes back to the previous screen. */
    onDone: () -> Unit = {},
    initialContactId: Long? = null,
    /** TIC-82: due tickle this compose was opened for; drives the mark-done
     *  prompt on return from the SMS handoff. Null when composing from the tab. */
    initialReminderId: Long? = null,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    LaunchedEffect(initialContactId, initialReminderId) {
        viewModel.attachReminder(initialReminderId, initialContactId)
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
    val warmth = com.xaymaca.sit.ui.theme.Warmth.Subtle
    val warmPalette = com.xaymaca.sit.ui.theme.WarmTheme.palette(warmth)
    // Send button + input focus accents follow the selected contact's
    // resolved canonical category — same logic as iOS ComposeView.
    // selectedContactCategoryId is a StateFlow combining the selected
    // contact + all groups-with-contacts; community fallback when
    // none resolves.
    val selectedCategoryId by viewModel.selectedContactCategoryId.collectAsState()
    val accent = remember(selectedCategoryId) {
        com.xaymaca.sit.ui.theme.WarmCategory.from(selectedCategoryId)
            ?.palette?.accent
            ?: com.xaymaca.sit.ui.theme.WarmCategory.Community.palette.accent
    }
    val warmFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = warmPalette.cardBg,
        focusedContainerColor = warmPalette.cardBg,
        unfocusedBorderColor = warmPalette.cardBorder,
        focusedBorderColor = accent,
        disabledContainerColor = warmPalette.cardBg,
        disabledBorderColor = warmPalette.cardBorder,
        cursorColor = accent,
        focusedTextColor = warmPalette.ink,
        unfocusedTextColor = warmPalette.ink,
        disabledTextColor = warmPalette.ink2,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        if (selectedContact != null || messageBody.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.clearCompose()
                                selectedTemplateName = null
                                onDone()
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.compose_clear),
                                    tint = warmPalette.ink2,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = warmPalette.paper,
                        titleContentColor = warmPalette.ink,
                    ),
                )
            },
            containerColor = warmPalette.paper,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    // Allow vertical scroll so the message field + send
                    // button stay reachable when the IME pushes content
                    // up. Without this, Column tries to fit everything
                    // into the cramped height and the message field's
                    // heightIn(min = 120.dp) gets overridden — it
                    // collapses to a single-line strip.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Inline warm 32sp title — parity with Network / Tickle /
                // Groups headers.
                Text(
                    text = stringResource(R.string.compose_title),
                    style = com.xaymaca.sit.ui.theme.WarmHeadingFont.style(
                        size = 32.sp,
                        warmth = warmth,
                    ).copy(color = warmPalette.ink),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                // To: contact search field
                com.xaymaca.sit.ui.warm.WarmEyebrow(
                    text = stringResource(R.string.compose_to_label),
                    warmth = warmth,
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                        placeholder = { Text(stringResource(R.string.compose_search_placeholder), color = warmPalette.ink3) },
                        singleLine = true,
                        shape = RoundedCornerShape(com.xaymaca.sit.ui.theme.WarmRadius.Surface),
                        enabled = selectedContact == null,
                        colors = warmFieldColors,
                    )
                    DropdownMenu(
                        expanded = showContactDropdown && contacts.isNotEmpty() && selectedContact == null,
                        onDismissRequest = { showContactDropdown = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.background(warmPalette.cardBg),
                    ) {
                        contacts.take(8).forEach { contact ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            contact.fullName.ifBlank { stringResource(R.string.common_no_name) },
                                            fontWeight = FontWeight.Medium,
                                            color = warmPalette.ink,
                                        )
                                        if (contact.company.isNotBlank()) {
                                            Text(
                                                contact.company,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = warmPalette.ink2,
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
                        label = { Text(selectedContact!!.fullName.ifBlank { stringResource(R.string.common_no_name) }) },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.clearContact() },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.compose_remove_contact),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.15f),
                            selectedLabelColor = accent,
                        ),
                    )
                    // No phone warning
                    val phones = stringListConverter.fromString(selectedContact!!.phoneNumbers)
                    if (phones.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.compose_no_phone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Template dropdown — hidden if no templates
                if (templates.isNotEmpty()) {
                    com.xaymaca.sit.ui.warm.WarmEyebrow(
                        text = stringResource(R.string.compose_template_label),
                        warmth = warmth,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { showTemplateDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(com.xaymaca.sit.ui.theme.WarmRadius.Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, warmPalette.cardBorder),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = warmPalette.cardBg,
                                contentColor = warmPalette.ink,
                            ),
                        ) {
                            Text(
                                text = selectedTemplateName ?: stringResource(R.string.compose_select_template),
                                modifier = Modifier.weight(1f),
                                color = if (selectedTemplateName == null) warmPalette.ink2 else warmPalette.ink,
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = warmPalette.ink2)
                        }
                        DropdownMenu(
                            expanded = showTemplateDropdown,
                            onDismissRequest = { showTemplateDropdown = false },
                            modifier = Modifier.background(warmPalette.cardBg),
                        ) {
                            templates.forEach { template ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(template.title, fontWeight = FontWeight.Medium, color = warmPalette.ink)
                                            Text(
                                                template.body.take(60) + if (template.body.length > 60) "…" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = warmPalette.ink2,
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
                com.xaymaca.sit.ui.warm.WarmEyebrow(
                    text = stringResource(R.string.compose_message_label),
                    warmth = warmth,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = viewModel::setMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .focusRequester(messageFocusRequester),
                    placeholder = { Text(stringResource(R.string.compose_message_placeholder), color = warmPalette.ink3) },
                    shape = RoundedCornerShape(com.xaymaca.sit.ui.theme.WarmRadius.Surface),
                    colors = warmFieldColors,
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
                            val phones = stringListConverter.fromString(contact.phoneNumbers)
                            val phone = phones.firstOrNull() ?: return@Button
                            val intent = SmsService().sendSmsIntent(context, listOf(phone), messageBody)
                            context.startActivity(intent)
                            viewModel.recordHandoff(contact)
                            viewModel.clearCompose()
                            selectedTemplateName = null
                            // Leave Compose underneath the SMS app so the user
                            // returns to the screen they came from, not an
                            // empty compose form.
                            onDone()
                        },
                        enabled = canSend,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = warmPalette.paperSurfaceAlt,
                            contentColor = androidx.compose.ui.graphics.Color(0xFFFAF4E2),
                            disabledContentColor = warmPalette.ink3,
                        ),
                        shape = RoundedCornerShape(com.xaymaca.sit.ui.theme.WarmRadius.Surface)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.common_send))
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

// Single source of truth for JSON-array parsing — the same converter Room uses
// for the phoneNumbers column. The old naive comma-split corrupted numbers that
// contained a comma (e.g. dial pauses).
private val stringListConverter = StringListConverter()
