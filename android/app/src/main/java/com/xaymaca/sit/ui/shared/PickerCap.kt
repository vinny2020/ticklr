package com.xaymaca.sit.ui.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.R

/**
 * TIC-96: a picker list capped at [PickerCap.DEFAULT_LIMIT] items, plus whether
 * more exist beyond the cap. ComposeScreen's recipient dropdown and
 * TickleEditScreen's contact picker both silently truncated to 8 with no
 * indication more contacts existed — this makes the cap honest via a
 * "Showing N of Total" hint row when [isTruncated].
 */
data class CappedList<T>(val shown: List<T>, val total: Int) {
    val isTruncated: Boolean get() = total > shown.size
}

object PickerCap {
    const val DEFAULT_LIMIT = 8

    fun <T> cap(items: List<T>, limit: Int = DEFAULT_LIMIT): CappedList<T> =
        CappedList(shown = items.take(limit), total = items.size)
}

/** Shared "Showing N of Total" hint row rendered under a truncated picker list. */
@Composable
fun PickerCapHintText(shown: Int, total: Int, color: Color) {
    Text(
        text = stringResource(R.string.picker_showing_hint, shown, total),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
