package com.xaymaca.sit.ui.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.ui.theme.Cobalt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    contactId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val isEditing = contactId != null

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var phoneNumbers by remember { mutableStateOf(listOf("")) }
    var emails by remember { mutableStateOf(listOf("")) }
    var notes by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var existingContact by remember { mutableStateOf<Contact?>(null) }

    // Load existing contact data if editing
    LaunchedEffect(contactId) {
        if (contactId != null) {
            val c = viewModel.getContactById(contactId)
            if (c != null) {
                existingContact = c
                firstName = c.firstName
                lastName = c.lastName
                company = c.company
                jobTitle = c.jobTitle
                notes = c.notes
                val listType = object : TypeToken<List<String>>() {}.type
                phoneNumbers = try {
                    (gson.fromJson(c.phoneNumbers, listType) as? List<String>)
                        ?.ifEmpty { listOf("") } ?: listOf("")
                } catch (e: Exception) { listOf("") }
                emails = try {
                    (gson.fromJson(c.emails, listType) as? List<String>)
                        ?.ifEmpty { listOf("") } ?: listOf("")
                } catch (e: Exception) { listOf("") }
                tags = try {
                    gson.fromJson(c.tags, listType) as? List<String> ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (isEditing) R.string.edit_contact_title else R.string.add_contact_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val cleanPhones = phoneNumbers.filter { it.isNotBlank() }
                                val cleanEmails = emails.filter { it.isNotBlank() }
                                if (isEditing && existingContact != null) {
                                    viewModel.updateContact(
                                        existingContact!!.copy(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            company = company.trim(),
                                            jobTitle = jobTitle.trim(),
                                            phoneNumbers = gson.toJson(cleanPhones),
                                            emails = gson.toJson(cleanEmails),
                                            notes = notes.trim(),
                                            tags = gson.toJson(tags)
                                        )
                                    )
                                } else {
                                    viewModel.addContact(
                                        Contact(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            company = company.trim(),
                                            jobTitle = jobTitle.trim(),
                                            phoneNumbers = gson.toJson(cleanPhones),
                                            emails = gson.toJson(cleanEmails),
                                            notes = notes.trim(),
                                            tags = gson.toJson(tags)
                                        )
                                    )
                                }
                                onSaved()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_save), color = Cobalt, fontWeight = FontWeight.SemiBold)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Name
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_name))
                SITTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = stringResource(R.string.add_contact_first_name)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SITTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = stringResource(R.string.add_contact_last_name)
                )
            }

            // Company / Job
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_work))
                SITTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = stringResource(R.string.add_contact_company)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SITTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = stringResource(R.string.add_contact_job_title)
                )
            }

            // Phone numbers
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_phone))
                phoneNumbers.forEachIndexed { index, phone ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SITTextField(
                            value = phone,
                            onValueChange = { newVal ->
                                phoneNumbers = phoneNumbers.toMutableList().also { it[index] = newVal }
                            },
                            label = stringResource(R.string.add_contact_phone_n, index + 1),
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(1f)
                        )
                        if (phoneNumbers.size > 1) {
                            IconButton(
                                onClick = {
                                    phoneNumbers = phoneNumbers.toMutableList().also { it.removeAt(index) }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(
                    onClick = { phoneNumbers = phoneNumbers + "" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_contact_add_phone))
                }
            }

            // Emails
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_email))
                emails.forEachIndexed { index, email ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SITTextField(
                            value = email,
                            onValueChange = { newVal ->
                                emails = emails.toMutableList().also { it[index] = newVal }
                            },
                            label = stringResource(R.string.add_contact_email_n, index + 1),
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.weight(1f)
                        )
                        if (emails.size > 1) {
                            IconButton(
                                onClick = {
                                    emails = emails.toMutableList().also { it.removeAt(index) }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(
                    onClick = { emails = emails + "" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_contact_add_email))
                }
            }

            // Notes
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_notes))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.add_contact_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Tags
            item {
                FormSectionHeader(stringResource(R.string.add_contact_section_tags))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SITTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = stringResource(R.string.add_contact_add_tag_label),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank() && !tags.contains(tagInput.trim())) {
                                tags = tags + tagInput.trim()
                                tagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_contact_add_tag_button))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (tags.isNotEmpty()) {
                    com.xaymaca.sit.ui.shared.TagChipRow(
                        tags = tags,
                        onRemove = { tag -> tags = tags.filter { it != tag } }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SITTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
