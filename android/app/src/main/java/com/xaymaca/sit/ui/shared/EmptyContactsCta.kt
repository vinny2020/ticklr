package com.xaymaca.sit.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.R

/**
 * TIC-96: shared dead-end fix for surfaces that need at least one contact to
 * do anything useful — ComposeScreen (nothing to address a message to) and
 * TickleEditScreen's contact picker (nothing to bind a tickle to) — and hit an
 * empty database with no way forward. [title] carries the surface-specific
 * framing; the CTA label and action are the same shape everywhere ("Add or
 * import contacts"), routed to whichever destination is more natural for that
 * surface by the caller.
 */
@Composable
fun EmptyContactsCta(
    title: String,
    accent: Color,
    onAddOrImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAddOrImport,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text(stringResource(R.string.common_add_or_import_contacts))
        }
    }
}
