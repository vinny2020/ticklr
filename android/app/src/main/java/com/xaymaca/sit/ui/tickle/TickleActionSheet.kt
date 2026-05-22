package com.xaymaca.sit.ui.tickle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency
import com.xaymaca.sit.ui.shared.displayNameResId
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme

/**
 * Connect-action sheet shown when a tickle row is tapped (TIC-36). Surfaces the
 * contact's available channels (Message / Call / Email) plus the secondary tickle
 * actions (Mark done / Snooze / Edit). Channel rows are hidden when the tickle has
 * no contact (group tickles) or the contact lacks that channel.
 *
 * Every action hands off to a system composer/dialer — no contact data leaves the
 * device. The hosting screen owns intent dispatch and dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickleActionSheet(
    target: TickleViewModel.TickleActionTarget,
    onCompose: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = WarmTheme.palette()
    val category = WarmCategory.from(target.categoryId) ?: WarmCategory.Community

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.paper) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: avatar + name + frequency
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.Lg, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(category.palette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = target.initials,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFAF4E2)),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = target.displayName,
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = palette.ink),
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(TickleFrequency.valueOf(target.reminder.frequency).displayNameResId),
                        style = TextStyle(fontSize = 13.sp, color = palette.ink2),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (target.phones.isNotEmpty()) {
                ActionRow(Icons.AutoMirrored.Filled.Message, stringResource(R.string.tickle_action_compose), category.palette.accent, onCompose)
                ActionRow(Icons.Default.Phone, stringResource(R.string.warm_contact_call), category.palette.accent, onCall)
            }
            if (target.emails.isNotEmpty()) {
                ActionRow(Icons.Default.Email, stringResource(R.string.contact_detail_section_email), category.palette.accent, onEmail)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(palette.cardBorder))

            ActionRow(Icons.Default.CheckCircle, stringResource(R.string.tickle_row_action_done), palette.ink2, onMarkDone)
            ActionRow(Icons.Default.Snooze, stringResource(R.string.tickle_row_action_snooze), palette.ink2, onSnooze)
            ActionRow(Icons.Default.Edit, stringResource(R.string.common_edit), palette.ink2, onEdit)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val palette = WarmTheme.palette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = WarmSpacing.Lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.ink),
        )
    }
}
