package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.theme.WarmRadius
import com.xaymaca.sit.ui.theme.WarmTheme

/** Cream-paper rounded container with hairline-separated rows. */
@Composable
fun WarmListContainer(
    modifier: Modifier = Modifier,
    warmth: Warmth = Warmth.Subtle,
    content: @Composable () -> Unit,
) {
    val palette = WarmTheme.palette(warmth)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WarmRadius.Surface))
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(WarmRadius.Surface)),
    ) {
        content()
    }
}

@Composable
fun WarmRowDivider(
    warmth: Warmth = Warmth.Subtle,
    leadingInset: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val palette = WarmTheme.palette(warmth)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = leadingInset)
            .height(1.dp)
            .background(palette.cardBorder),
    )
}
