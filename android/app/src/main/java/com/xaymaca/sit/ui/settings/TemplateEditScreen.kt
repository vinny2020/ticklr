package com.xaymaca.sit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.ui.theme.Cobalt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: Long,
    onBack: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val existing = remember(templates, templateId) {
        if (templateId == -1L) null else templates.find { it.id == templateId }
    }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var body by remember(existing) { mutableStateOf(existing?.body ?: "") }

    val isEditing = templateId != -1L
    val canSave = title.isNotBlank() && body.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Template" else "New Template", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canSave) {
                                if (isEditing && existing != null) {
                                    viewModel.updateTemplate(existing.copy(title = title.trim(), body = body.trim()))
                                } else {
                                    viewModel.saveTemplate(title, body)
                                }
                                onBack()
                            }
                        },
                        enabled = canSave
                    ) {
                        Text(
                            if (isEditing) "Update" else "Save",
                            color = if (canSave) Cobalt else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                label = { Text("Body") },
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
