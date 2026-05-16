package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.WarmCategory

/** Circular avatar with initials, tinted to the category accent. List row size. */
@Composable
fun MonogramAvatar(
    initials: String,
    category: WarmCategory,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val palette = category.palette
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.uppercase(),
            style = TextStyle(
                fontSize = (size.value * 0.40f).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFAF4E2),
            ),
        )
    }
}

/**
 * Empty-state photo affordance for the Contact Detail header. Dashed
 * circle in the category accent + faded monogram + camera badge.
 * Spec: HANDOFF.md lines 22-30.
 */
@Composable
fun MonogramPhotoAffordance(
    initials: String,
    category: WarmCategory,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
) {
    val palette = category.palette
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Dashed circle border
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 2f
            drawCircle(
                color = palette.accent.copy(alpha = 0.55f),
                radius = (size.toPx() - strokeWidth) / 2f,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                ),
            )
        }
        // Faded monogram
        Text(
            text = initials.uppercase(),
            style = TextStyle(
                fontSize = (size.value * 0.32f).sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent.copy(alpha = 0.32f),
            ),
        )
        // Camera badge bottom-right
        val badgeSize = size * 0.27f
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = size * 0.03f, y = size * 0.03f)
                .size(badgeSize)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(badgeSize * 0.5f),
            )
        }
    }
}
