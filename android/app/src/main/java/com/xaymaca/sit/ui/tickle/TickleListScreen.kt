package com.xaymaca.sit.ui.tickle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.data.model.TickleReminder
import com.xaymaca.sit.ui.shared.displayNameResId
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmPalette
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.warm.WarmCard
import com.xaymaca.sit.ui.warm.WarmCardVariant
import com.xaymaca.sit.ui.warm.WarmEyebrow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleListScreen(
    onAddTickle: () -> Unit,
    onEditTickle: (Long) -> Unit,
    onCompose: (Long) -> Unit,
    // When true (tablet two-pane), the host renders the connect actions in the
    // detail pane instead of this modal bottom sheet. Tap still resolves the same
    // `actionTarget` on the shared ViewModel; only the presentation differs.
    twoPane: Boolean = false,
    viewModel: TickleViewModel = hiltViewModel(),
) {
    val warmth = Warmth.Subtle
    val palette = WarmTheme.palette(warmth)
    val dueReminders by viewModel.dueReminders.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val snoozedReminders by viewModel.snoozedReminders.collectAsState()
    val reminderDisplays by viewModel.reminderDisplays.collectAsState()
    val actionTarget by viewModel.actionTarget.collectAsState()
    val fallbackDisplay = TickleViewModel.RowDisplay(initials = "T", name = "Tickle")
    val context = LocalContext.current

    Scaffold(
        containerColor = palette.paper,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTickle,
                containerColor = WarmCategory.Milestones.palette.accent,
                contentColor = palette.paper,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tickle_list_add_fab))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WarmHeader(palette = palette, warmth = warmth, dueCount = dueReminders.size)
            }

            item {
                WarmCard(
                    category = WarmCategory.Milestones,
                    variant = WarmCardVariant.Hero,
                    warmth = warmth,
                    showPrompt = true,
                    onClick = onAddTickle,
                    modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
                )
            }

            if (dueReminders.isNotEmpty()) {
                item {
                    WarmEyebrow(
                        text = stringResource(R.string.tickle_list_section_due),
                        warmth = warmth,
                        modifier = Modifier.padding(start = WarmSpacing.Lg, top = 4.dp),
                    )
                }
                items(dueReminders, key = { it.id }) { reminder ->
                    val display = reminderDisplays[reminder.id] ?: fallbackDisplay
                    TickleRow(
                        reminder = reminder,
                        display = display,
                        isDue = true,
                        palette = palette,
                        onClick = { viewModel.onTickleTapped(reminder) },
                        onComplete = { viewModel.markComplete(reminder) },
                        onSnooze = { viewModel.snooze(reminder) },
                        onDelete = { viewModel.delete(reminder) },
                    )
                }
            }

            if (upcomingReminders.isNotEmpty()) {
                item {
                    WarmEyebrow(
                        text = stringResource(R.string.tickle_list_section_upcoming),
                        warmth = warmth,
                        modifier = Modifier.padding(start = WarmSpacing.Lg, top = 8.dp),
                    )
                }
                items(upcomingReminders, key = { it.id }) { reminder ->
                    val display = reminderDisplays[reminder.id] ?: fallbackDisplay
                    TickleRow(
                        reminder = reminder,
                        display = display,
                        isDue = false,
                        palette = palette,
                        onClick = { viewModel.onTickleTapped(reminder) },
                        onComplete = { viewModel.markComplete(reminder) },
                        onSnooze = { viewModel.snooze(reminder) },
                        onDelete = { viewModel.delete(reminder) },
                    )
                }
            }

            if (snoozedReminders.isNotEmpty()) {
                item {
                    WarmEyebrow(
                        text = stringResource(R.string.tickle_list_section_snoozed),
                        warmth = warmth,
                        modifier = Modifier.padding(start = WarmSpacing.Lg, top = 8.dp),
                    )
                }
                items(snoozedReminders, key = { it.id }) { reminder ->
                    val display = reminderDisplays[reminder.id] ?: fallbackDisplay
                    Box(modifier = Modifier.alpha(0.55f)) {
                        TickleRow(
                            reminder = reminder,
                            display = display,
                            isDue = false,
                            palette = palette,
                            onClick = { viewModel.onTickleTapped(reminder) },
                            onComplete = { viewModel.markComplete(reminder) },
                            onSnooze = { viewModel.snooze(reminder) },
                            onDelete = { viewModel.delete(reminder) },
                        )
                    }
                }
            }
        }

        actionTarget?.takeIf { !twoPane }?.let { target ->
            TickleActionSheet(
                target = target,
                onCompose = {
                    val contactId = target.reminder.contactId
                    viewModel.dismissActionSheet()
                    if (contactId != null) onCompose(contactId)
                },
                onCall = {
                    target.phones.firstOrNull()?.let { number ->
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    }
                    viewModel.dismissActionSheet()
                },
                onEmail = {
                    target.emails.firstOrNull()?.let { email ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                    }
                    viewModel.dismissActionSheet()
                },
                onMarkDone = { viewModel.markComplete(target.reminder); viewModel.dismissActionSheet() },
                onSnooze = { viewModel.snooze(target.reminder); viewModel.dismissActionSheet() },
                onEdit = { onEditTickle(target.reminder.id); viewModel.dismissActionSheet() },
                onDismiss = { viewModel.dismissActionSheet() },
            )
        }
    }
}

@Composable
private fun WarmHeader(palette: WarmPalette, warmth: Warmth, dueCount: Int) {
    Column(
        modifier = Modifier.padding(start = WarmSpacing.Lg, end = WarmSpacing.Lg, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.tickle_list_title),
            style = WarmHeadingFont.style(32.sp, warmth).copy(color = palette.ink),
        )
        Text(
            text = if (dueCount > 0) {
                // Literal copy until a plural-aware string lands.
                "$dueCount to reach out to today."
            } else {
                stringResource(R.string.warm_tickle_subtitle_empty)
            },
            style = TextStyle(fontSize = 14.sp, color = palette.ink2),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TickleRow(
    reminder: TickleReminder,
    display: TickleViewModel.RowDisplay,
    isDue: Boolean,
    palette: WarmPalette,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = WarmCategory.from(display.categoryId) ?: WarmCategory.Community
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onComplete(); true }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                else -> false
            }
        },
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            scope.launch { dismissState.reset() }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val target = dismissState.targetValue
            val bg = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> category.palette.accent
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFB2422C)
                else -> palette.paper
            }
            Box(
                modifier = Modifier.fillMaxSize().background(bg),
                contentAlignment = when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ) {
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.tickle_row_action_done),
                        tint = Color(0xFFFAF4E2),
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    SwipeToDismissBoxValue.EndToStart -> Row(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = stringResource(R.string.tickle_row_action_snooze),
                            tint = Color.White,
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.tickle_row_action_delete),
                            tint = Color.White,
                        )
                    }
                    else -> {}
                }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.cardBg)
                .clickable(onClick = onClick)
                .padding(horizontal = WarmSpacing.Lg, vertical = WarmSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category-tinted avatar; due rings get an extra 2pt outline.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(category.palette.accent)
                    .then(
                        if (isDue) Modifier.border(2.dp, category.palette.accent, CircleShape)
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = display.initials,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFAF4E2),
                    ),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = display.name,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                    maxLines = 1,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val freq = stringResource(TickleFrequency.valueOf(reminder.frequency).displayNameResId)
                    val pillBg = if (isDue) category.palette.accent else category.palette.accentTint
                    val pillFg = if (isDue) Color(0xFFFAF4E2) else category.palette.accent
                    Text(
                        text = freq,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pillFg),
                    )
                    Text(
                        text = relativeDateLabel(reminder.nextDueDate),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (isDue) category.palette.accent else palette.ink2,
                        ),
                    )
                }
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = TextStyle(fontSize = 12.sp, color = palette.ink3),
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onComplete) {
                Icon(
                    imageVector = if (isDue) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.tickle_row_action_done),
                    tint = if (isDue) category.palette.accent else palette.ink3,
                )
            }
        }
    }
}

@Composable
private fun relativeDateLabel(timestamp: Long): String {
    val diffDays = calendarDayDelta(System.currentTimeMillis(), timestamp)
    return when {
        diffDays == 0 -> stringResource(R.string.tickle_row_today)
        diffDays == 1 -> stringResource(R.string.tickle_row_tomorrow)
        diffDays == -1 -> stringResource(R.string.tickle_row_yesterday)
        diffDays > 1 -> stringResource(R.string.tickle_row_in_days, diffDays)
        else -> stringResource(R.string.tickle_row_days_overdue, abs(diffDays))
    }
}

internal fun calendarDayDelta(now: Long, target: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val targetDate = Instant.ofEpochMilli(target).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(today, targetDate).toInt()
}

