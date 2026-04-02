package com.xaymaca.sit.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onImport: () -> Unit,
    onResetOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sendDirectly by viewModel.sendDirectly.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val seedMessage by viewModel.seedMessage.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var tempThemeMode by remember(showThemeDialog) { mutableIntStateOf(themeMode) }
    
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0"
        }
    }

    LaunchedEffect(seedMessage) {
        seedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSeedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .padding(vertical = 8.dp)
        ) {
            // Appearance section
            SettingsSectionHeader("Appearance")

            val themeLabel = when (themeMode) {
                1 -> "Light"
                2 -> "Dark"
                else -> "System Default"
            }

            SettingsRow(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = themeLabel,
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Data section
            SettingsSectionHeader("Data")

            SettingsRow(
                icon = Icons.Default.CloudUpload,
                title = "Import Contacts",
                subtitle = "From phone or LinkedIn CSV",
                onClick = onImport
            )

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Messaging section
            SettingsSectionHeader("Messaging")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = Cobalt,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Send SMS directly",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Uses SmsManager instead of opening Messages app. Android advantage over iOS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sendDirectly,
                    onCheckedChange = { viewModel.toggleSendDirectly() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = Cobalt
                    )
                )
            }

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // About section
            SettingsSectionHeader("About")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "Ticklr",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Built by Xaymaca",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Privacy-first. No cloud, no analytics, no account required. All data is stored on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Danger zone
            SettingsSectionHeader("Developer")

            SettingsRow(
                icon = Icons.Default.Refresh,
                title = "Reset Onboarding",
                subtitle = "Show the welcome screen again",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { showResetConfirm = true }
            )

            if (viewModel.isDebug) {
                HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = "Load Test Contacts",
                    subtitle = "Seeds 20 fake contacts from assets (debug only)",
                    iconTint = Amber,
                    onClick = { viewModel.loadTestContacts() }
                )
                HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Contacts",
                    subtitle = "Permanently deletes all contacts (debug only)",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showClearConfirm = true }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeOption("System Default", tempThemeMode == 0) {
                        tempThemeMode = 0
                    }
                    ThemeOption("Light", tempThemeMode == 1) {
                        tempThemeMode = 1
                    }
                    ThemeOption("Dark", tempThemeMode == 2) {
                        tempThemeMode = 2
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setThemeMode(tempThemeMode)
                        showThemeDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Onboarding") },
            text = { Text("This will return you to the welcome screen. Your data will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetOnboarding()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Contacts") },
            text = { Text("This will permanently delete all contacts. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllContacts()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = Cobalt,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
