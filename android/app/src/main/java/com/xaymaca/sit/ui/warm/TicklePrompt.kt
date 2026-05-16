package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.theme.WarmTheme

/**
 * Decorative strip at the bottom of a warm card. Non-interactive — the
 * "Tickle idea: …" copy is a gentle suggestion, not a tap target. Mirrors
 * the iOS TicklePrompt after the user-requested revert.
 */
@Composable
fun TicklePrompt(
    category: WarmCategory,
    modifier: Modifier = Modifier,
    warmth: Warmth = Warmth.Subtle,
) {
    val palette = category.palette
    val warmPalette = WarmTheme.palette(warmth)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accentTint)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
        Text(
            text = stringResource(R.string.warm_card_tickleIdea),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent,
            ),
        )
        Text(
            text = stringResource(category.promptShortRes),
            style = TextStyle(
                fontSize = 12.sp,
                color = warmPalette.ink2,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
