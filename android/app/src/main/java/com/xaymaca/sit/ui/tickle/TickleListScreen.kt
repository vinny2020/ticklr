package com.xaymaca.sit.ui.tickle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleListScreen(
    onAddTickle: () -> Unit,
    onEditTickle: (Long) -> Unit,
    viewModel: TickleViewModel = hiltViewModel()
) {
    val dueReminders by viewModel.dueReminders.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val snoozedReminders by viewModel.snoozedReminders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tickle", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTickle,
                containerColor = Amber,
                contentColor = MaterialTheme.colorScheme.background
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add tickle")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val allEmpty = dueReminders.isEmpty() && upcomingReminders.isEmpty() && snoozedReminders.isEmpty()
        if (allEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No tickles yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to schedule a reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (dueReminders.isNotEmpty()) {
                    item {
                        TickleSectionHeader("Due", color = Amber)
                    }
                    items(dueReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            isDue = true,
                            onClick = { onEditTickle(reminder.id) },
                            onComplete = { viewModel.markComplete(reminder) },
                            onSnooze = { viewModel.snooze(reminder) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                        HorizontalDivider(color = NavyLight, thickness = 0.5.dp)
                    }
                }

                if (upcomingReminders.isNotEmpty()) {
                    item {
                        TickleSectionHeader("Upcoming")
                    }
                    items(upcomingReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            isDue = false,
                            onClick = { onEditTickle(reminder.id) },
                            onComplete = { viewModel.markComplete(reminder) },
                            onSnooze = { viewModel.snooze(reminder) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                        HorizontalDivider(color = NavyLight, thickness = 0.5.dp)
                    }
                }

                if (snoozedReminders.isNotEmpty()) {
                    item {
                        TickleSectionHeader("Snoozed")
                    }
                    items(snoozedReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            isDue = false,
                            onClick = { onEditTickle(reminder.id) },
                            onComplete = { viewModel.markComplete(reminder) },
                            onSnooze = { viewModel.snooze(reminder) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                        HorizontalDivider(color = NavyLight, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TickleSectionHeader(title: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TickleReminderRow(
    reminder: TickleReminder,
    isDue: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onComplete()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    // After a complete swipe (StartToEnd), reset the row so it animates back.
    // The item will move to Upcoming via DB update; delete stays dismissed.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            scope.launch { dismissState.reset() }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val targetValue = dismissState.targetValue
            val bgColor = when (targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Amber
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.background
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = when (targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                when (targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    SwipeToDismissBoxValue.EndToStart -> Row(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = "Snooze",
                            tint = Color.White
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                    else -> {}
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials avatar placeholder (contact name resolved at ViewModel level in production)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDue) Amber else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDue) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.note.ifBlank { "Tickle reminder" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = relativeDateLabel(reminder.nextDueDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDue) Amber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Frequency badge
            Badge(
                containerColor = if (isDue) Amber else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isDue) MaterialTheme.colorScheme.background
                               else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = reminder.frequency.lowercase()
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun relativeDateLabel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = timestamp - now
    val diffDays = (diffMs / (1000L * 60 * 60 * 24)).toInt()

    return when {
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        diffDays == -1 -> "Yesterday"
        diffDays > 1 -> "In ${diffDays}d"
        else -> "${abs(diffDays)}d overdue"
    }
}
