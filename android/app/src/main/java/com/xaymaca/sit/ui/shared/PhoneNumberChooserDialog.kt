package com.xaymaca.sit.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.R

/**
 * TIC-96: shown whenever [PhoneChooser] resolves more than one number for a
 * contact — the Send/Call action can't guess which the user meant, so this
 * lists every number and hands the picked one back via [onSelect].
 */
@Composable
fun PhoneNumberChooserDialog(
    numbers: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.phone_chooser_title)) },
        text = {
            Column {
                numbers.forEach { number ->
                    Text(
                        text = number,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(number) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
