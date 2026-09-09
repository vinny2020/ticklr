package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.R

/** Text-free, native-drawn art choices for user-created groups (TIC-107). */
enum class CustomGroupStyle(
    val id: String,
    val label: String,
    val artRes: Int,
) {
    Studio("studio", "Studio & Ideas", R.drawable.custom_group_studio),
    Garden("garden", "Garden & Growth", R.drawable.custom_group_garden),
    Journeys("journeys", "Journeys", R.drawable.custom_group_journeys),
    Books("books", "Books & Learning", R.drawable.custom_group_books),
    Creative("creative", "Creative Practice", R.drawable.custom_group_creative),
    Gathering("gathering", "Food & Gathering", R.drawable.custom_group_gathering),
    Wellness("wellness", "Wellness", R.drawable.custom_group_wellness),
    Sports("sports", "Sports & Play", R.drawable.custom_group_sports),
    Night("night", "Night Out", R.drawable.custom_group_night),
    ;

    companion object {
        fun from(id: String?): CustomGroupStyle = entries.firstOrNull { it.id == id } ?: Studio
    }
}

@Composable
fun CustomGroupArtwork(styleId: String?, modifier: Modifier = Modifier) {
    val style = CustomGroupStyle.from(styleId)
    androidx.compose.foundation.Image(
        painter = painterResource(style.artRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).fillMaxSize(),
    )
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
