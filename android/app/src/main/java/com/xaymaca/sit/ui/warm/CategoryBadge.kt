package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmRadius

/** 56dp rounded square holding a category icon, tinted to the category badge color. */
@Composable
fun CategoryBadge(
    category: WarmCategory,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val palette = category.palette
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(WarmRadius.Badge))
            .background(palette.accentBadge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(size * 0.4f),
        )
    }
}
