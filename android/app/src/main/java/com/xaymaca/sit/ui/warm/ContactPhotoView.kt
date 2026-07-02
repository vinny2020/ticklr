package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.service.ContactPhotoService
import com.xaymaca.sit.service.LocalPhotoStore
import com.xaymaca.sit.service.StringListConverter
import com.xaymaca.sit.ui.theme.WarmCategory

enum class ContactPhotoStyle { List, Detail }

/**
 * Resolves a contact's avatar through this priority chain (per HANDOFF
 * lines 8-49):
 *   1. user-attached local photo (LocalPhotoStore)
 *   2. system Contacts photo (ContactPhotoService)
 *   3. monogram (List style) or full empty-state photo affordance
 *      (Detail style)
 *
 * Mirrors iOS ContactPhotoView.
 */
@Composable
fun ContactPhotoView(
    contact: Contact,
    category: WarmCategory,
    modifier: Modifier = Modifier,
    style: ContactPhotoStyle = ContactPhotoStyle.List,
    size: Dp? = null,
    refreshKey: Any = Unit,
) {
    val context = LocalContext.current
    val resolvedSize = size ?: when (style) {
        ContactPhotoStyle.List -> 36.dp
        ContactPhotoStyle.Detail -> 132.dp
    }
    var resolved by remember(contact.id, refreshKey) { mutableStateOf<Resolution>(Resolution.Loading) }

    LaunchedEffect(contact.id, refreshKey) {
        val local = LocalPhotoStore.read(context, contact.id)
        if (local != null) {
            resolved = Resolution.Image(local.asImageBitmap())
            return@LaunchedEffect
        }
        val system = ContactPhotoService.fetch(
            context = context,
            contactId = contact.id,
            phoneNumbers = stringListConverter.fromString(contact.phoneNumbers),
            emails = stringListConverter.fromString(contact.emails),
        )
        resolved = if (system != null) Resolution.Image(system.asImageBitmap()) else Resolution.Empty
    }

    Box(
        modifier = modifier.size(resolvedSize),
        contentAlignment = Alignment.Center,
    ) {
        when (val r = resolved) {
            Resolution.Loading -> Box(
                modifier = Modifier
                    .size(resolvedSize)
                    .clip(CircleShape)
                    .background(category.palette.accentTint),
            )
            is Resolution.Image -> Image(
                bitmap = r.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(resolvedSize)
                    .clip(CircleShape),
            )
            Resolution.Empty -> when (style) {
                ContactPhotoStyle.List -> MonogramAvatar(
                    initials = contact.initials(),
                    category = category,
                    size = resolvedSize,
                )
                ContactPhotoStyle.Detail -> MonogramPhotoAffordance(
                    initials = contact.initials(),
                    category = category,
                    size = resolvedSize,
                )
            }
        }
    }
}

private sealed interface Resolution {
    data object Loading : Resolution
    data class Image(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : Resolution
    data object Empty : Resolution
}

private fun Contact.initials(): String {
    val pieces = listOfNotNull(
        firstName.takeIf { it.isNotBlank() }?.first()?.toString(),
        lastName.takeIf { it.isNotBlank() }?.first()?.toString(),
    )
    return if (pieces.isEmpty()) "?" else pieces.joinToString("").uppercase()
}

// Single source of truth for JSON-array parsing — the same converter Room uses
// for the stored columns, so phone/email values round-trip exactly (the old
// hand-rolled unescaper mangled escaped quotes/backslashes).
private val stringListConverter = StringListConverter()
