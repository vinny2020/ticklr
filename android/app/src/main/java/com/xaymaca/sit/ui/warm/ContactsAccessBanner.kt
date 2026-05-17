package com.xaymaca.sit.ui.warm

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.Contact
import com.xaymaca.sit.ui.theme.WarmCategory

/**
 * Tappable banner shown below the photo affordance on Contact Detail
 * when READ_CONTACTS permission isn't granted yet. Tap behavior:
 *
 *   - First time, never asked → launches system permission prompt.
 *   - User denied once (rationale should be shown) → launches prompt
 *     again.
 *   - User denied with "Don't ask again" → deep-links to the app's
 *     system Settings page.
 *   - Granted → banner hides automatically.
 *
 * Hidden when the contact has no phone numbers or emails (no system
 * match would have been possible regardless of permission). Re-checks
 * permission state on lifecycle resume so returning from Settings with
 * a fresh grant invalidates the photo cache without another tap.
 */
@Composable
fun ContactsAccessBanner(
    contact: Contact,
    category: WarmCategory,
    modifier: Modifier = Modifier,
    onGranted: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var permissionState by remember {
        mutableStateOf(currentPermissionState(context))
    }
    var userAttemptedOnce by remember { mutableStateOf(false) }

    // Re-check status when the screen resumes (e.g. user returns from
    // system Settings after flipping the permission).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val previously = permissionState
                val now = currentPermissionState(context)
                permissionState = now
                if (previously != PermissionState.Granted && now == PermissionState.Granted) {
                    onGranted()
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        userAttemptedOnce = true
        permissionState = if (granted) PermissionState.Granted else PermissionState.Denied
        if (granted) onGranted()
    }

    val hasMatchableInfo = remember(contact.id) {
        contact.phoneNumbers.isNotBlank() && contact.phoneNumbers != "[]"
            || contact.emails.isNotBlank() && contact.emails != "[]"
    }

    if (permissionState == PermissionState.Granted || !hasMatchableInfo) return

    val palette = category.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accentTint)
            .clickable {
                handleTap(
                    context = context,
                    activity = activity,
                    userAttemptedOnce = userAttemptedOnce,
                    launcher = { launcher.launch(Manifest.permission.READ_CONTACTS) },
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.padding(end = 0.dp),
        )
        Text(
            text = stringResource(R.string.warm_contact_accessBanner),
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.accent),
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = palette.accent,
        )
    }
}

// MARK: - State helpers

private enum class PermissionState { Granted, Denied }

private fun currentPermissionState(context: android.content.Context): PermissionState =
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
    ) PermissionState.Granted else PermissionState.Denied

private fun handleTap(
    context: android.content.Context,
    activity: Activity?,
    userAttemptedOnce: Boolean,
    launcher: () -> Unit,
) {
    if (activity == null) { launcher(); return }
    val rationaleAllowed = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.READ_CONTACTS,
    )
    when {
        // Either the user hasn't been asked yet, or they previously
        // denied once and the system will show the prompt again.
        rationaleAllowed || !userAttemptedOnce -> launcher()
        // Permanently denied — system prompt won't reappear. Deep-link
        // to the app's Settings page so they can flip it manually.
        else -> openAppSettings(context)
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
