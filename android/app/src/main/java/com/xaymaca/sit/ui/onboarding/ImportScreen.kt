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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.network.NetworkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onImportSuccess: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isImporting by remember { mutableStateOf(false) }

    // TIC-85: a successful import (whether 0, 1, or N contacts landed) now
    // auto-advances to the Network tab immediately via onImportSuccess()
    // instead of showing an in-place success card + "Continue" button — that
    // extra tap was pure ceremony since the import had already happened. The
    // count/duplicates summary travels via NetworkViewModel's
    // PendingSnackbarMessageStore post (TIC-84 pattern) and is surfaced by the
    // app-level snackbar host after navigation, not here. isImporting is only
    // reset in the failure branches below — on success onImportSuccess()
    // tears this screen down before another recomposition would observe it
    // reset. onImportSuccess and onSkip are distinct callbacks (not one
    // onComplete) because they land on different tabs — Network for a real
    // import, Tickle for "Skip for now" (its empty state carries the hero CTA).

    // Permission launcher for READ_CONTACTS
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                isImporting = true
                try {
                    viewModel.importFromContacts()
                    onImportSuccess()
                } catch (e: Exception) {
                    isImporting = false
                    snackbarHostState.showSnackbar(context.getString(R.string.import_snackbar_failed, e.message ?: ""))
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.import_snackbar_permission_denied))
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
                        viewModel.importFromCSV(inputStream)
                        onImportSuccess()
                    } else {
                        isImporting = false
                        snackbarHostState.showSnackbar(context.getString(R.string.import_snackbar_could_not_open))
                    }
                } catch (e: Exception) {
                    isImporting = false
                    snackbarHostState.showSnackbar(context.getString(R.string.import_snackbar_failed, e.message ?: ""))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Import from Phone Contacts button
            ImportOptionCard(
                icon = Icons.Default.Contacts,
                title = stringResource(R.string.import_from_phone_title),
                subtitle = stringResource(R.string.import_from_phone_subtitle),
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        coroutineScope.launch {
                            isImporting = true
                            try {
                                viewModel.importFromContacts()
                                onImportSuccess()
                            } catch (e: Exception) {
                                isImporting = false
                                snackbarHostState.showSnackbar(context.getString(R.string.import_snackbar_failed, e.message ?: ""))
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
                title = stringResource(R.string.import_from_linkedin_title),
                subtitle = stringResource(R.string.import_from_linkedin_subtitle),
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
                        stringResource(R.string.import_linkedin_instructions_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.import_linkedin_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // TIC-85: success no longer routes through here — it auto-advances
            // via onImportSuccess() from the import handlers above. This
            // button is now exclusively the "Skip for now" escape hatch.
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.import_skip_button), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                tint = MaterialTheme.colorScheme.primary,
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
