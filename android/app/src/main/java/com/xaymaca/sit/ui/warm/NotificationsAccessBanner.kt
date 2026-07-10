package com.xaymaca.sit.ui.warm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.ui.theme.WarmCategory

/**
 * Banner shown on the Tickle list when POST_NOTIFICATIONS isn't granted
 * (Android 13+), meaning reminder notifications are silently dropped by
 * TickleWorker/TickleAlarmReceiver. Mount it only when the user has at
 * least one reminder so the ask is contextual.
 *
 * On first mount ever (tracked in SharedPreferences) it launches the
 * system permission prompt directly — the user just created or owns
 * reminders, so this is the moment of highest intent. After that:
 *
 *   - Denied once (rationale allowed) → tap re-launches the prompt.
 *   - Permanently denied → tap deep-links to the app's notification
 *     settings so the user can flip the toggle manually.
 *   - Granted → banner hides automatically (re-checked on resume, so
 *     returning from Settings with a fresh grant dismisses it).
 */
@Composable
fun NotificationsAccessBanner(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val activity = context as? Activity

    var granted by remember { mutableStateOf(hasNotificationsPermission(context)) }
    var userAttemptedOnce by remember { mutableStateOf(false) }

    // Re-check on resume (e.g. user returns from system Settings).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = hasNotificationsPermission(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result ->
        userAttemptedOnce = true
        granted = result
    }

    // One-time contextual auto-prompt: the banner is only mounted when
    // reminders exist, so ask directly the first time instead of waiting
    // for a tap.
    LaunchedEffect(Unit) {
        if (granted) return@LaunchedEffect
        val prefs = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_NOTIFICATIONS_PROMPTED, false)) {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_PROMPTED, true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (granted) return

    val palette = WarmCategory.Milestones.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accentTint)
            .clickable {
                handleTap(
                    activity = activity,
                    context = context,
                    userAttemptedOnce = userAttemptedOnce,
                    launcher = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = palette.accent,
        )
        Text(
            text = stringResource(R.string.warm_tickle_notificationsBanner),
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

private const val KEY_NOTIFICATIONS_PROMPTED = "notifications_prompted"

private fun hasNotificationsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun handleTap(
    activity: Activity?,
    context: Context,
    userAttemptedOnce: Boolean,
    launcher: () -> Unit,
) {
    if (activity == null) { launcher(); return }
    val rationaleAllowed = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS,
    )
    when {
        // Not asked yet this install, or denied once — the system prompt
        // will still appear.
        rationaleAllowed || !userAttemptedOnce -> launcher()
        // Permanently denied — the prompt won't reappear; send the user
        // to the app's notification settings instead.
        else -> openNotificationSettings(context)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
