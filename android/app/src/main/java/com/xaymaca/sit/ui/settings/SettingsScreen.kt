package com.xaymaca.sit.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.shared.WordmarkLockup
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onImport: () -> Unit,
    onTemplates: () -> Unit,
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
            packageInfo.versionName ?: "1.0"
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {
            // Appearance section
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))

            val themeLabel = when (themeMode) {
                1 -> stringResource(R.string.settings_theme_light)
                2 -> stringResource(R.string.settings_theme_dark)
                else -> stringResource(R.string.settings_theme_system)
            }

            SettingsRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme_title),
                subtitle = themeLabel,
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Data section
            SettingsSectionHeader(stringResource(R.string.settings_section_data))

            SettingsRow(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.settings_import_title),
                subtitle = stringResource(R.string.settings_import_subtitle),
                onClick = onImport
            )

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            SettingsRow(
                icon = Icons.AutoMirrored.Filled.TextSnippet,
                title = stringResource(R.string.settings_templates_title),
                subtitle = stringResource(R.string.settings_templates_subtitle),
                onClick = onTemplates
            )

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Messaging section
            SettingsSectionHeader(stringResource(R.string.settings_section_messaging))

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
                        stringResource(R.string.settings_sms_direct_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.settings_sms_direct_subtitle),
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
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            WordmarkLockup()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.settings_about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.settings_about_built_by),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_about_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))

            // Danger zone
            SettingsSectionHeader(stringResource(R.string.settings_section_developer))

            SettingsRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_reset_onboarding_title),
                subtitle = stringResource(R.string.settings_reset_onboarding_subtitle),
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { showResetConfirm = true }
            )

            if (viewModel.isDebug) {
                HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.settings_debug_load_title),
                    subtitle = stringResource(R.string.settings_debug_load_subtitle),
                    iconTint = Amber,
                    onClick = { viewModel.loadTestContacts() }
                )
                HorizontalDivider(color = NavyLight, modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.settings_clear_data_title),
                    subtitle = stringResource(R.string.settings_debug_clear_subtitle),
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showClearConfirm = true }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_choose_theme)) },
            text = {
                Column {
                    ThemeOption(stringResource(R.string.settings_theme_system), tempThemeMode == 0) {
                        tempThemeMode = 0
                    }
                    ThemeOption(stringResource(R.string.settings_theme_light), tempThemeMode == 1) {
                        tempThemeMode = 1
                    }
                    ThemeOption(stringResource(R.string.settings_theme_dark), tempThemeMode == 2) {
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
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_onboarding_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetOnboarding()
                    }
                ) {
                    Text(stringResource(R.string.common_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_data_title)) },
            text = { Text(stringResource(R.string.settings_clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllContacts()
                    }
                ) {
                    Text(stringResource(R.string.settings_clear_data_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
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
