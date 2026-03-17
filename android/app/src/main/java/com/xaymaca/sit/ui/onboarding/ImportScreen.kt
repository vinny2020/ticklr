package com.xaymaca.sit.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.ui.network.NetworkViewModel
import com.xaymaca.sit.ui.theme.Cobalt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isImporting by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf<Int?>(null) }

    // Permission launcher for READ_CONTACTS
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val count = viewModel.importFromContacts()
                    importedCount = count
                    snackbarHostState.showSnackbar("Imported $count contacts")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Contacts permission denied")
            }
        }
    }

    // File picker launcher for LinkedIn CSV
    val csvPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val count = viewModel.importFromCSV(inputStream)
                        importedCount = count
                        snackbarHostState.showSnackbar("Imported $count LinkedIn contacts")
                    } else {
                        snackbarHostState.showSnackbar("Could not open file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isImporting) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Cobalt)
                }
            }

            importedCount?.let { count ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Successfully imported $count contacts",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Import from Phone Contacts button
            ImportOptionCard(
                icon = Icons.Default.Contacts,
                title = "Import from Phone Contacts",
                subtitle = "Sync your contacts from the device address book.",
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        coroutineScope.launch {
                            isImporting = true
                            try {
                                val count = viewModel.importFromContacts()
                                importedCount = count
                                snackbarHostState.showSnackbar("Imported $count contacts")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Import failed: ${e.message}")
                            } finally {
                                isImporting = false
                            }
                        }
                    } else {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                enabled = !isImporting
            )

            // Import from LinkedIn CSV button
            ImportOptionCard(
                icon = Icons.Default.FileOpen,
                title = "Import from LinkedIn CSV",
                subtitle = "Export your connections from LinkedIn and select the CSV file.",
                onClick = {
                    csvPickerLauncher.launch("text/*")
                },
                enabled = !isImporting
            )

            // LinkedIn export instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to export from LinkedIn",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Go to linkedin.com\n" +
                        "2. Tap Me → Settings & Privacy\n" +
                        "3. Data Privacy → Get a copy of your data\n" +
                        "4. Select \"Connections\" and request export\n" +
                        "5. Download the CSV file and select it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue button
            if (importedCount != null) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue to Network", fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Cobalt,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
