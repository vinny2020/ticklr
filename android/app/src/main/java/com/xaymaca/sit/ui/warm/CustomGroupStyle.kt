package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Text-free, native-drawn art choices for user-created groups (TIC-107). */
enum class CustomGroupStyle(
    val id: String,
    val label: String,
    val symbol: String,
    val colors: List<Color>,
) {
    Studio("studio", "Studio & Ideas", "✦", listOf(Color(0xFFEED6C2), Color(0xFFC98672))),
    Garden("garden", "Garden & Growth", "❋", listOf(Color(0xFFD8E6C8), Color(0xFF7CA776))),
    Journeys("journeys", "Journeys", "⌁", listOf(Color(0xFFD8E5EF), Color(0xFF6F98B5))),
    Books("books", "Books & Learning", "▤", listOf(Color(0xFFE7D9B7), Color(0xFF9A7145))),
    Creative("creative", "Creative Practice", "✎", listOf(Color(0xFFE4D3E8), Color(0xFF9D719F))),
    Gathering("gathering", "Food & Gathering", "◒", listOf(Color(0xFFF1D4B9), Color(0xFFBE704E))),
    Wellness("wellness", "Wellness", "☼", listOf(Color(0xFFD2E6E1), Color(0xFF568E88))),
    Sports("sports", "Sports & Play", "◉", listOf(Color(0xFFD7E0F2), Color(0xFF5E7BB5))),
    Night("night", "Night Out", "☾", listOf(Color(0xFFDCD7EE), Color(0xFF5E578C))),
    ;

    companion object {
        fun from(id: String?): CustomGroupStyle = entries.firstOrNull { it.id == id } ?: Studio
    }
}

@Composable
fun CustomGroupArtwork(styleId: String?, modifier: Modifier = Modifier) {
    val style = CustomGroupStyle.from(styleId)
    Box(
        modifier = modifier
            .background(Brush.linearGradient(style.colors))
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative paper-like layers keep this compact and scale cleanly at any density.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.30f))
        )
        Text(style.symbol, fontSize = 38.sp, color = Color.White.copy(alpha = 0.9f))
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .width(46.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.65f))
        )
    }
}

@Composable
fun CustomGroupStylePicker(selectedStyleId: String?, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(350.dp),
    ) {
        items(CustomGroupStyle.entries, key = { it.id }) { style ->
            val selected = style.id == selectedStyleId
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(style.id) }
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                CustomGroupArtwork(style.id, Modifier.fillMaxWidth().aspectRatio(1.35f))
                Text(
                    text = style.label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }
        }
    }
}
