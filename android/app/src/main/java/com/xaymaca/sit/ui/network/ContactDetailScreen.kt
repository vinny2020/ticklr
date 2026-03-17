package com.xaymaca.sit.ui.network

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    onAddTickle: () -> Unit,
    onEdit: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var contact by remember { mutableStateOf<Contact?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val gson = remember { Gson() }

    LaunchedEffect(contactId) {
        contact = viewModel.getContactById(contactId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.fullName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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
            val phoneNumbers: List<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(c.phoneNumbers, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }

            val emails: List<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(c.emails, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }

            val tags: List<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(c.tags, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }

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
                            text = c.fullName.ifBlank { "No Name" },
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

                // Add Tickle button
                item {
                    Button(
                        onClick = onAddTickle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Tickle", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Phone numbers
                if (phoneNumbers.isNotEmpty()) {
                    item { SectionHeader("Phone") }
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
                    item { SectionHeader("Email") }
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
                        SectionHeader("Notes")
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
                        SectionHeader("Tags")
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
                        Text("Delete Contact", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val c = contact
        if (c != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Contact") },
                text = { Text("Remove ${c.fullName} from your network? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteContact(c)
                            showDeleteDialog = false
                            onBack()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
