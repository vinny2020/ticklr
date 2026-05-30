package com.xaymaca.sit.ui.shared

import androidx.compose.foundation.background
import com.xaymaca.sit.ui.theme.LocalIsAppDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.BebasNeue

/**
 * Ticklr wordmark lockup — warm "Open Ink" theme.
 *
 * Replaces the retired navy/amber card with an open, editorial lockup: the
 * Bebas Neue wordmark in warm ink, a hairline rule above the
 * "YOUR PEOPLE MATTER" tagline. No background — adapts to its surface and
 * flips ink colors for dark mode.
 *
 * @param wordmarkSize size of the Bebas wordmark (drives all other metrics)
 * @param showRule     hairline divider; auto-suppressed below ~36sp
 * @param dot          optional terracotta full-stop ("Ticklr.") — one warm accent
 */
@Composable
fun WordmarkLockup(
    modifier: Modifier = Modifier,
    wordmarkSize: TextUnit = 64.sp,
    showRule: Boolean = true,
    dot: Boolean = false
) {
    val dark = LocalIsAppDark.current
    val ink        = if (dark) Color(0xFFF4EFE3) else Color(0xFF1A1F2A) // wordmark
    val ink2       = if (dark) Color(0xFFB8A98E) else Color(0xFF5C6470) // tagline
    val ink3       = if (dark) Color(0xFF7C6C50) else Color(0xFF9099A4) // rule
    val terracotta = if (dark) Color(0xFFD08A6A) else Color(0xFFB26342) // optional dot

    val sz = wordmarkSize.value
    val taglineSize = (sz * 0.17f).coerceAtLeast(9f).sp
    val ruleVisible = showRule && sz >= 36f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((sz * 0.16f).dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Ticklr",
                style = TextStyle(
                    fontFamily = BebasNeue,
                    fontWeight = FontWeight.Normal,
                    fontSize = wordmarkSize,
                    lineHeight = wordmarkSize,
                    letterSpacing = (-0.012).em,
                    color = ink
                )
            )
            if (dot) {
                Spacer(Modifier.width((sz * 0.06f).dp))
                Box(
                    Modifier
                        .padding(bottom = (sz * 0.12f).dp)
                        .size((sz * 0.15f).dp)
                        .clip(CircleShape)
                        .background(terracotta)
                )
            }
        }
        if (ruleVisible) {
            Box(
                Modifier
                    .width((sz * 0.65f).dp)
                    .height(1.dp)
                    .background(ink3.copy(alpha = if (dark) 0.6f else 0.5f))
            )
        }
        Text(
            text = "YOUR PEOPLE MATTER",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = taglineSize,
                letterSpacing = 0.34.em,
                color = ink2
            )
        )
    }
}
