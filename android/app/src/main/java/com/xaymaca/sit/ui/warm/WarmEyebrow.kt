package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.theme.WarmTheme

/** Small uppercase tracked label used above sections and inside cards. */
@Composable
fun WarmEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    warmth: Warmth = Warmth.Subtle,
) {
    val palette = WarmTheme.palette(warmth)
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (11 * 0.16).sp,
            color = palette.ink3,
        ),
    )
}
