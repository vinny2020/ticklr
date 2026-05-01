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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.ui.shared.displayNameResId
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.Cobalt
import com.xaymaca.sit.ui.theme.NavyLight
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
    val reminderDisplays by viewModel.reminderDisplays.collectAsState()
    val fallbackDisplay = TickleViewModel.RowDisplay(initials = "T", name = "Tickle")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tickle_list_title), fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tickle_list_add_fab))
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
                        stringResource(R.string.tickle_list_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.tickle_list_empty_description),
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
                        TickleSectionHeader(stringResource(R.string.tickle_list_section_due), color = Amber)
                    }
                    items(dueReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            display = reminderDisplays[reminder.id] ?: fallbackDisplay,
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
                        TickleSectionHeader(stringResource(R.string.tickle_list_section_upcoming))
                    }
                    items(upcomingReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            display = reminderDisplays[reminder.id] ?: fallbackDisplay,
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
                        TickleSectionHeader(stringResource(R.string.tickle_list_section_snoozed))
                    }
                    items(snoozedReminders, key = { it.id }) { reminder ->
                        TickleReminderRow(
                            reminder = reminder,
                            display = reminderDisplays[reminder.id] ?: fallbackDisplay,
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
    display: TickleViewModel.RowDisplay,
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
                        contentDescription = stringResource(R.string.tickle_row_action_done),
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    SwipeToDismissBoxValue.EndToStart -> Row(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = stringResource(R.string.tickle_row_action_snooze),
                            tint = Color.White
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.tickle_row_action_delete),
                            tint = Color.White
                        )
                    }
                    else -> {}
                }
            }
        }
    ) {
        val dateLabel = relativeDateLabel(reminder.nextDueDate)
        val frequencyLabel = stringResource(TickleFrequency.valueOf(reminder.frequency).displayNameResId)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDue) Amber else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = display.initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDue) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Layout mirrors iOS TickleRowView: bold contact/group name on top,
            // frequency badge + due-date label inline below, optional note last.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = display.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Badge(
                        containerColor = if (isDue) Amber else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isDue) MaterialTheme.colorScheme.background
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = frequencyLabel,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDue) Amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Check button — quick complete without swipe. Matches iOS row.
            IconButton(onClick = onComplete) {
                Icon(
                    imageVector = if (isDue) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.tickle_row_action_done),
                    tint = if (isDue) Amber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun relativeDateLabel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = timestamp - now
    val diffDays = (diffMs / (1000L * 60 * 60 * 24)).toInt()
    return when {
        diffDays == 0 -> stringResource(R.string.tickle_row_today)
        diffDays == 1 -> stringResource(R.string.tickle_row_tomorrow)
        diffDays == -1 -> stringResource(R.string.tickle_row_yesterday)
        diffDays > 1 -> stringResource(R.string.tickle_row_in_days, diffDays)
        else -> stringResource(R.string.tickle_row_days_overdue, abs(diffDays))
    }
}
