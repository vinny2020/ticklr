# Fix: Compose Screen — Two Bug Fixes

## Overview
Two bugs reported from the Compose flow. Both fixes are in `ComposeScreen.kt` and `ComposeViewModel.kt`.

---

## Bug 1 — Direct SMS toggle has no effect (Messages app always opens)

### Root cause
`ComposeViewModel.sendDirectly` is a `MutableStateFlow` initialized **once** from `SharedPreferences` at ViewModel construction time:

```kotlin
// ComposeViewModel.kt line 56-59 — BROKEN
val sendDirectly: StateFlow<Boolean> = MutableStateFlow(
    context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)
)
```

It never re-reads the pref after the ViewModel is created. The Settings screen writes to `SharedPreferences` correctly via `SettingsViewModel.toggleSendDirectly()`, but `ComposeViewModel` never sees the update.

### Fix — `ComposeViewModel.kt`

Remove the stale `sendDirectly` StateFlow entirely. Instead, read the pref **at send time** inside `ComposeScreen.kt`.

**Step 1 — Delete `sendDirectly` from `ComposeViewModel`:**

Remove these lines (56–59):
```kotlin
val sendDirectly: StateFlow<Boolean> = MutableStateFlow(
    context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)
)
```

**Step 2 — Update `ComposeScreen.kt` send button logic:**

Remove:
```kotlin
val sendDirectly by viewModel.sendDirectly.collectAsState()
```

In the Send button `onClick`, replace the `sendDirectly` reference with a fresh pref read:
```kotlin
onClick = {
    val contact = selectedContact ?: return@Button
    val phones = parseJsonStringArray(contact.phoneNumbers)
    val phone = phones.firstOrNull() ?: return@Button
    val smsService = SmsService()
    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    // Read pref fresh at send time — not from a stale ViewModel StateFlow
    val sendDirectly = context
        .getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)

    if (sendDirectly && hasSmsPermission) {
        smsService.sendSms(context, phone, messageBody)
    } else {
        val intent = smsService.sendSmsIntent(context, listOf(phone), messageBody)
        context.startActivity(intent)
    }
    viewModel.clearCompose()
    selectedTemplateName = null
    viewModel.showToast("Message sent \u2713")
},
```

---

## Bug 2 — Typing in contact search field loses focus (keyboard dismisses)

### Root cause
The `DropdownMenu` composable is **focusable by default**. When `showContactDropdown` flips to `true` and the menu appears, it steals focus from the `OutlinedTextField`, causing the keyboard to dismiss mid-typing.

### Fix — `ComposeScreen.kt`

Add `properties = PopupProperties(focusable = false)` to the contact search `DropdownMenu`.

**Before (line 103):**
```kotlin
DropdownMenu(
    expanded = showContactDropdown && contacts.isNotEmpty() && selectedContact == null,
    onDismissRequest = { showContactDropdown = false }
) {
```

**After:**
```kotlin
DropdownMenu(
    expanded = showContactDropdown && contacts.isNotEmpty() && selectedContact == null,
    onDismissRequest = { showContactDropdown = false },
    properties = PopupProperties(focusable = false)
) {
```

**Required import** — add to the imports block at the top of `ComposeScreen.kt`:
```kotlin
import androidx.compose.ui.window.PopupProperties
```

---

## Files to Modify
- `app/src/main/java/com/xaymaca/sit/ui/compose/ComposeViewModel.kt`
- `app/src/main/java/com/xaymaca/sit/ui/compose/ComposeScreen.kt`

## Verification
1. Toggle "Send SMS directly" ON in Settings → go to Compose → send a message → should send silently via SmsManager (no Messages app).
2. Toggle OFF → send → Messages app opens pre-filled. ✓
3. Type in the "To:" search field → dropdown appears → keyboard stays open, cursor remains active. ✓
