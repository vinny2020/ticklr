package com.xaymaca.sit.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.BebasNeue
import com.xaymaca.sit.ui.theme.Navy

@Composable
fun WordmarkLockup(
    modifier: Modifier = Modifier,
    wordmarkSize: TextUnit = 48.sp,
    taglineSize: TextUnit = 11.sp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Navy)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ticklr",
            style = TextStyle(
                fontFamily = BebasNeue,
                fontWeight = FontWeight.Normal,
                fontSize = wordmarkSize,
                lineHeight = wordmarkSize,
                letterSpacing = (-0.02).em,
                color = Amber
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "YOUR PEOPLE MATTER",
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = taglineSize,
                letterSpacing = 0.4.em,
                color = Amber.copy(alpha = 0.5f)
            )
        )
    }
}
