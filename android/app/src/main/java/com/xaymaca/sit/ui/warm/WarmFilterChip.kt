package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.theme.WarmTheme

sealed class FilterChipKind {
    object All : FilterChipKind()
    data class CategoryKind(val category: WarmCategory) : FilterChipKind()

    /** TIC-88: a user-created group filter chip. Neutral pill styling (like the
     *  All chip) rather than a canonical category's accent, so custom groups read
     *  as distinct from the four canonical categories. */
    object GroupKind : FilterChipKind()
}

/**
 * Pill-shaped filter chip used on the warm Network screen. Mirrors
 * iOS WarmFilterChip — label locks to one line + intrinsic width so it
 * never collapses inside a horizontally scrolling row.
 */
@Composable
fun WarmFilterChip(
    kind: FilterChipKind,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    warmth: Warmth = Warmth.Subtle,
) {
    val palette = WarmTheme.palette(warmth)

    // GroupKind shares the neutral All-chip styling (ink when active, cardBg
    // otherwise) so custom groups read distinctly from canonical categories.
    val neutralActive = (kind is FilterChipKind.All || kind is FilterChipKind.GroupKind) && isActive
    val neutralInactive = (kind is FilterChipKind.All || kind is FilterChipKind.GroupKind) && !isActive
    val background = when {
        neutralActive -> palette.ink
        neutralInactive -> palette.cardBg
        kind is FilterChipKind.CategoryKind && isActive -> kind.category.palette.accent
        kind is FilterChipKind.CategoryKind && !isActive -> kind.category.palette.accentTint
        else -> palette.cardBg
    }
    val foreground = when {
        neutralActive -> palette.paper
        neutralInactive -> palette.ink
        kind is FilterChipKind.CategoryKind && isActive -> Color(0xFFFAF4E2)
        kind is FilterChipKind.CategoryKind && !isActive -> kind.category.palette.accent
        else -> palette.ink
    }
    val borderColor = when {
        kind is FilterChipKind.CategoryKind && !isActive -> palette.cardBorder
        kind is FilterChipKind.GroupKind && !isActive -> palette.cardBorder
        else -> Color.Transparent
    }

    val countBg = when {
        neutralActive -> palette.paper.copy(alpha = 0.18f)
        neutralInactive -> palette.ink.copy(alpha = 0.06f)
        kind is FilterChipKind.CategoryKind && isActive -> Color.White.copy(alpha = 0.22f)
        kind is FilterChipKind.CategoryKind && !isActive -> kind.category.palette.accentBadge
        else -> palette.ink.copy(alpha = 0.06f)
    }

    Row(
        modifier = modifier
            .wrapContentWidth()
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (kind is FilterChipKind.CategoryKind) {
            // Size matches the 13sp label glyph height so CategoryKind
            // chips don't render taller than the icon-less All chip.
            Icon(
                imageVector = kind.category.icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(countBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "$count",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = foreground,
                    ),
                )
            }
        }
    }
}
