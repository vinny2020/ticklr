# CLAUDE.md — Ticklr Android

---

## 🔴 CRITICAL FIX REQUIRED — Gson TypeToken R8 Crash (Production)

**Crashlytics issue:** `9309f0532c76b10b7a2073eaac91d5bc`
**Affects:** Release builds only (Play Store). Debug builds are unaffected.
**Version introduced:** 1.4.3 (versionCode 18)

### Root Cause
`ContactFingerprint.kt` uses `TypeToken<List<String>>` to deserialize JSON via Gson.
R8 (the release build shrinker) strips generic type signatures at compile time.
At runtime Gson cannot determine the target type → `IllegalStateException` → crash.

```
Caused by java.lang.IllegalStateException: TypeToken must be created with a type argument:
new TypeToken<...>() {}; When using code shrinkers (ProGuard, R8, ...) make sure
that generic signatures are preserved.
  at com.google.gson.reflect.TypeToken.getTypeTokenTypeArgument
  at p7.b.<clinit>   ← ContactFingerprint
```

### Fix — Two files

**1. `service/ContactFingerprint.kt`**

Remove all Gson usage entirely. Replace with a simple string parser that is R8-safe:

- Remove imports: `com.google.gson.Gson`, `com.google.gson.reflect.TypeToken`
- Remove fields: `private val gson = Gson()` and `private val listType = ...`
- Replace the two `runCatching { gson.fromJson(...) }` blocks with calls to a new
  private helper function:

```kotlin
private fun parseJsonStringArray(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
    return trimmed
        .removePrefix("[")
        .removeSuffix("]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
}
```

Then replace:
```kotlin
// OLD — crashes in release
val phones: List<String> = try {
    gson.fromJson<List<String>>(phoneNumbersJson, listType) ?: emptyList()
} catch (e: Exception) { emptyList() }

val emails: List<String> = try {
    gson.fromJson<List<String>>(emailsJson, listType) ?: emptyList()
} catch (e: Exception) { emptyList() }
```

With:
```kotlin
// NEW — R8-safe, no Gson needed
val phones: List<String> = parseJsonStringArray(phoneNumbersJson)
val emails: List<String> = parseJsonStringArray(emailsJson)
```

**2. `app/proguard-rules.pro`**

Add as a safety net for any other Gson TypeToken usage in the project:

```proguard
# Preserve Gson generic signatures so R8 doesn't strip them
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
```

### Why debug builds didn't crash
Debug builds skip R8/ProGuard minification entirely — generic signatures are preserved.
Release builds run R8 → signatures stripped → Gson TypeToken fails at runtime.

### After fixing
- Bump versionCode and versionName
- Run `./gradlew bundleRelease tagRelease` to build, tag, and push
- Verify in Firebase Crashlytics that issue `9309f0532c76b10b7a2073eaac91d5bc` stops occurring

---

## 🐛 Bugs & Improvements — ComposeScreen (from device testing)

The following issues were observed on a physical Android device on `ComposeScreen`.

---

### 🔴 Critical: Can't Send Message on ComposeScreen (reported by beta tester)

**What's broken:**
A beta tester reports being completely unable to send a message from `ComposeScreen`. This is
likely the same root cause as the "0 selected" bug below — if selection state isn't tracked,
the Send button stays disabled and no SMS is ever dispatched. Treat these two as one fix.

---

### 🔴 Critical: Swipe-to-Complete on TickleListScreen Leaves Row Stuck in Amber State

**What's broken:**
On `TickleListScreen`, swiping right on a tickle row reveals an Amber checkmark action. After
tapping/triggering it, the tickle is marked complete in the DB but the row remains expanded and
stuck showing the Amber swipe background — it does not animate out or collapse. The list only
resets to a normal state after the app is fully closed and reopened.

**Observed behavior:** Swipe right → Amber check revealed → action triggered → row frozen in
expanded amber state. Full app restart required to recover.

**Likely cause:**
The swipe dismissal state is managed by `SwipeToDismissBox` (or `SwipeToDismiss`) and its
`DismissState` / `AnchoredDraggableState` is not being reset after the action completes.
In Compose, completing the action does not automatically snap the item back — you must
explicitly call `reset()` on the state, or remove and re-add the item to trigger recomposition.

**Fix:**
After the complete action is confirmed:
1. Call `state.reset()` (or `snapTo(DismissValue.Default)` in a coroutine) to animate the
   row back to its resting position, OR
2. If the tickle moves to a different section (e.g., from Upcoming → completed/hidden), ensure
   the `LazyColumn` key for that item changes so Compose removes and replaces it cleanly
3. Do NOT rely on the DB update alone to trigger recomposition of the swipe state — swipe
   state is local UI state and must be reset explicitly

**Files likely involved:**
- `ui/tickle/TickleListScreen.kt` — swipe action handler + dismiss state reset
- `ui/tickle/TickleViewModel.kt` — `completeTickle()` should emit a signal the screen can
  use to reset swipe state after the suspend function returns

---

### 🔴 Critical: Recipient Count Shows "0 selected" Despite Checked Contacts — STILL BROKEN

**What's broken:**
Confirmed still failing on physical device after a previous fix attempt. Contacts appear checked
(checkbox is visually filled/blue) but the counter reads `"0 recipients selected"` and the Send
button remains disabled. The fix did not work — do a full audit, not a patch.

**Required: read both files completely before touching anything**
Open `ui/compose/ComposeScreen.kt` and `ui/compose/ComposeViewModel.kt` and trace the full
data flow from checkbox tap → ViewModel → state → UI update. Do not guess — find the actual break.

**Audit checklist — verify each of these explicitly:**

1. **ViewModel state shape**
   - Does `ComposeViewModel` have a `StateFlow<Set<Long>>` (or `Set<Contact>`) for selected IDs?
   - Is it a `MutableStateFlow` internally, exposed as read-only `StateFlow`?
   - Is it initialized as `MutableStateFlow(emptySet())`?

2. **Toggle function**
   - Does `toggleContact(contact: Contact)` actually mutate the `MutableStateFlow`?
   - Is it using `.update { current -> if (id in current) current - id else current + id }`?
   - Is it called on the correct `contact.id` (Room `Long` PK), not object reference?

3. **Screen collection**
   - Is `ComposeScreen` collecting the StateFlow with `collectAsStateWithLifecycle()`?
   - Is the collected value used as the source of truth for both the checkbox AND the count label?
   - Is there any `remember { mutableStateOf(...) }` local checkbox state that shadows the ViewModel?

4. **LazyColumn key**
   - Does the `LazyColumn` use `key = { contact -> contact.id }` on each item?
   - Without stable keys, Compose may recompose items incorrectly and lose checked state visually

5. **Count label and Send button**
   - Are both derived from `selectedIds.size`, not from a separate counter variable?

6. **The checkmark visual vs actual state**
   - If the checkbox *looks* checked but state says 0, the checkbox is reading local state while
     the count reads ViewModel state — they are definitely decoupled. Find where the local state
     is being set and remove it entirely. The checkbox `checked` param must come from ViewModel.

**After the fix, verify on device:**
- Tap a contact → counter increments to 1, Send enables
- Tap same contact again → counter decrements to 0, Send disables
- Tap multiple contacts → count reflects exact number checked
- Send button dispatches SMS to the correct set of recipients

---

### 🟡 TickleEditScreen: Search Field Stops Returning Results After a Contact Is Selected

**What's broken:**
On `TickleEditScreen`, after selecting a contact the user can still type in the contact search
field, but no results appear. The user cannot search for a different contact to replace the
current selection.

**Expected behavior:**
- Selecting a contact sets it as the current selection AND clears the search query
- The search field remains fully functional — typing again should filter and return results
- The user can replace the selected contact by searching and tapping a new one

**Likely cause:**
After a contact is selected, the search query `StateFlow` or `MutableState` is not being reset,
OR the contact list is being filtered against a stale/empty result set, OR the list is hidden
entirely once a selection is made. The search field being visible but returning nothing suggests
the filter logic is broken post-selection rather than the field being disabled.

**Fix:**
1. On contact selection, call `viewModel.clearSearch()` (or equivalent) to reset the query to
   an empty string — this should re-show the full unfiltered contact list
2. Ensure `filteredContacts` reacts to the cleared query and returns all contacts again
3. Do NOT hide or disable the search field after selection — keep it active so the user can
   replace their choice
4. The selected contact display (chip, name label, etc.) should be separate from the search
   field state — selecting should update the selection UI without affecting search functionality

**Files likely involved:**
- `ui/tickle/TickleEditScreen.kt` — contact search field + selection handler
- `ui/tickle/TickleViewModel.kt` — search query reset on selection

---

### 🟡 Save Confirmation: No Feedback After Saving on TickleEditScreen

**What's broken:**
Tapping "Save" on the New Tickle / Edit Tickle screen (`TickleEditScreen`) saves successfully
but the user receives no visual confirmation. Reported by a beta tester on the New Tickle screen
(Group tab, with group "Investment", frequency "Daily", note "check in").

**Fix:**
Same `Box`-overlay toast pattern as ComposeScreen (see below). After a successful save:
- Message: `"Tickle saved"` (or `"Tickle updated"` if editing an existing one)
- Auto-dismiss after 2 seconds
- If a shared `TicklrToast` composable exists in `ui/shared/`, use it here too

**Files likely involved:**
- `ui/tickle/TickleEditScreen.kt` — add toast overlay
- `ui/tickle/TickleViewModel.kt` — expose `toastMessage: StateFlow<String?>`

---

### 🟡 Save Confirmation: No Feedback After Saving a Template

**What's broken:**
Tapping "Save" on the template/message saves successfully but the user receives no acknowledgment.
The action feels broken or ignored.

**Fix:**
After a successful save, show a brief toast overlay (same `Box`-overlay pattern used for the
group member confirmation — not a Scaffold Snackbar):
- Message: `"Template saved"` or `"Message saved"` depending on context
- Auto-dismiss after 2 seconds
- Same styling: Cobalt `#2563EB` background, white text, 12dp rounded corners, bottom-center

If a shared `ToastOverlay` composable was already created for the group member feature, reuse it
here. Otherwise create a reusable `TicklrToast(message: String?, onDismiss: () -> Unit)` composable
in `ui/shared/` so both screens can use it.

**Files likely involved:**
- `ui/compose/ComposeScreen.kt` — add toast overlay
- `ui/compose/ComposeViewModel.kt` — expose `toastMessage: StateFlow<String?>`
- `ui/shared/` — consider extracting a shared `TicklrToast` composable

---

### 🟢 Enhancement: Add Search + Sort to ComposeScreen Contact List

**What's needed:**
With a large contact list (1,800+ contacts), the current flat scroll is unusable. Users need
to be able to filter and sort.

**Search:**
- Add a `SearchBar` or `OutlinedTextField` at the top of the contact list (below the message
  field, above the recipient count)
- Filter contacts in real time by name or company as the user types
- Filtering should work against `firstName`, `lastName`, and `company` fields
- Implement filter logic in `ComposeViewModel` — expose a `searchQuery: MutableStateFlow<String>`
  and a `filteredContacts: StateFlow<List<Contact>>` derived from it

**Sort:**
- Add a sort control (icon button or dropdown) near the search bar
- Sort options: **A–Z** (default, by last name), **Z–A**, **Recently Added**
- Persist selected sort within the session (no need for `@AppStorage` persistence)

**Parity note:** iOS `ComposeView` already has search on this screen — this brings Android to
feature parity.

**Files likely involved:**
- `ui/compose/ComposeScreen.kt` — search field + sort control UI
- `ui/compose/ComposeViewModel.kt` — `searchQuery`, `sortOrder`, `filteredContacts` StateFlows

---

## Architecture — MVVM + Repository
- **UI**: Jetpack Compose (no XML layouts)
- **State**: ViewModel + StateFlow
- **Persistence**: Room (version 2) + TypeConverters for List<String>
- **DI**: Hilt
- **Navigation**: Navigation Compose (NavGraph.kt)
- **Background work**: WorkManager (TickleWorker)
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 35

## Project Structure

```
app/src/main/java/com/xaymaca/sit/
├── SITApp.kt                       # @HiltAndroidApp, PREFS_NAME, KEY_ONBOARDING_COMPLETE
├── MainActivity.kt                 # Entry point, NavGraph host
├── data/
│   ├── model/
│   │   ├── Contact.kt              # Room @Entity
│   │   ├── ContactGroup.kt         # Room @Entity
│   │   ├── ContactGroupCrossRef.kt # Many-to-many join table
│   │   ├── MessageTemplate.kt      # Room @Entity
│   │   ├── TickleReminder.kt       # Room @Entity
│   │   ├── Enums.kt                # ImportSource, TickleFrequency, TickleStatus
│   │   └── Relations.kt            # ContactWithGroups, GroupWithContacts
│   ├── dao/
│   │   ├── ContactDao.kt
│   │   ├── ContactGroupDao.kt
│   │   ├── MessageTemplateDao.kt
│   │   └── TickleReminderDao.kt
│   ├── db/
│   │   └── SITDatabase.kt          # Room DB, version 2, StringListConverter
│   └── repository/
│       ├── ContactRepository.kt
│       ├── MessageTemplateRepository.kt
│       └── TickleRepository.kt
├── di/
│   └── DatabaseModule.kt           # Hilt module — provides DB and DAOs
├── service/
│   ├── ContactImportService.kt     # ContactsContract import
│   ├── LinkedInCSVParser.kt        # CSV parsing (mirrors iOS implementation)
│   ├── SmsService.kt               # SmsManager direct send + Intent fallback
│   ├── StringListConverter.kt      # Room TypeConverter for List<String>
│   ├── TickleScheduler.kt          # nextDueDate logic + WorkManager scheduling
│   └── TickleWorker.kt             # WorkManager Worker for notifications
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # Navy, Cobalt, Amber + variants
│   │   ├── Theme.kt                # SITTheme (dark-first, Material3)
│   │   └── Type.kt                 # Typography
│   ├── nav/
│   │   ├── NavGraph.kt             # Full NavHost + BottomNavigation
│   │   └── Screen.kt              # Sealed class for all routes
│   ├── launch/
│   │   └── LaunchScreen.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   └── ImportScreen.kt
│   ├── network/
│   │   ├── NetworkListScreen.kt    # Searchable contact list
│   │   ├── NetworkViewModel.kt
│   │   ├── ContactDetailScreen.kt
│   │   └── AddContactScreen.kt     # Create + edit (reused with contactId param)
│   ├── groups/
│   │   ├── GroupListScreen.kt
│   │   ├── GroupDetailScreen.kt
│   │   └── GroupViewModel.kt
│   ├── tickle/
│   │   ├── TickleListScreen.kt     # Due/Upcoming/Snoozed sections
│   │   ├── TickleEditScreen.kt
│   │   └── TickleViewModel.kt
│   ├── compose/
│   │   ├── ComposeScreen.kt
│   │   └── ComposeViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── shared/
│       └── TagChipRow.kt
└── tests/
    ├── LinkedInCSVParserTest.kt    # Unit tests — passing
    ├── StringListConverterTest.kt  # Unit tests — passing
    └── TickleSchedulerTest.kt      # Unit tests — passing
```


## What's Complete ✅ — Full Android Feature Set

- Room database — all 5 entities, 4 DAOs, StringListConverter, version 2
- Hilt DI — DatabaseModule providing all repositories
- Compose theme — Navy/Cobalt/Amber Pulse identity, dark-first Material3
- Full NavGraph — 5-tab bottom nav + all nested routes with proper back stack
- `NetworkListScreen` — searchable contact list
- `ContactDetailScreen` — full detail view with tickle shortcut
- `AddContactScreen` — create + edit (reused via contactId param)
- `GroupListScreen` + `GroupDetailScreen`
- `TickleListScreen` — Due/Upcoming/Snoozed sections
- `TickleEditScreen` — create/edit with contact/group picker, frequency, date, note
- `TickleScheduler` — nextDueDate logic + WorkManager scheduling
- `TickleWorker` — WorkManager Worker for background notifications
- `ComposeScreen` + `ComposeViewModel` — multi-select + SMS send
- `SmsService` — SmsManager direct send + Intent fallback
- `ImportScreen` — LinkedIn CSV + contacts import
- `ContactImportService` — ContactsContract import
- `LinkedInCSVParser` — mirrors iOS implementation
- `SettingsScreen` + `SettingsViewModel` — includes debug "Load Test Contacts" button
- `SeedDataService` — DEBUG only, loads `test_contacts.csv` from assets
- `LaunchScreen` — Pulse identity splash
- `OnboardingScreen`
- Unit tests — LinkedInCSVParser, StringListConverter, TickleScheduler, ScreenRoute, ContactRepository (all passing)
- Build artifacts present — app has been compiled and built successfully
- Screenshot prep — `./gradlew screenshotPrep` sets 9:41, full signal, 100% battery, no notifications
- Screenshot teardown — `./gradlew screenshotTeardown` restores normal status bar

### Android — Nice to Have
- **SmsManager direct send UX** — surface the "send directly vs open Messages" preference in Settings
- **Search on ComposeScreen** — parity with iOS (filter contacts while selecting recipients)

---

## 🔄 UX Copy Update: Set Expectations on LinkedIn Import

**What to change:**
LinkedIn imports only bring in name + company + job title — no phone numbers, rarely an email.
Users may be confused when imported contacts appear "empty." Update the copy in `ImportScreen`
to frame LinkedIn import as **relationship seeding**, not a full contact sync.

**Suggested language (use as a guide, polish the tone to match the app's voice):**
- Heading or subtitle: `"Seed your network from LinkedIn"`
- Helper text: `"LinkedIn exports include names and companies — no phone numbers.
  You'll add contact details manually after importing."`
- Or as a callout card: `"LinkedIn data gives you the who. You fill in the how to reach them."`

**Where:** `ui/onboarding/ImportScreen.kt` — update the LinkedIn section description copy only.
Do not change the step-by-step instructions, only the framing copy above or around them.

---

## 🚧 Enhancement: Contact Reachability Icons on Network List

### What to Build
In `NetworkListScreen` (and any contact row composable), show small inline icons next to each
contact indicating how they can be reached:

- ✉ Envelope icon — contact has at least one email address
- 📞 Phone icon — contact has at least one phone number
- Both icons — contact has both
- No icons — contact has neither (LinkedIn-only seed data)

**Design:**
- Use Material Icons: `Icons.Default.Email` and `Icons.Default.Phone`
- Size: `16.dp`, color: `Color.White.copy(alpha = 0.45f)` (subtle, non-distracting)
- Position: trailing end of the contact row, laid out in a `Row` with `4.dp` spacing
- Must not clutter the row — keep icons small and muted

**Logic:**
- Email present: `contact.emails.isNotEmpty()`
- Phone present: `contact.phoneNumbers.isNotEmpty()`

**Files likely involved:**
- `ui/network/NetworkListScreen.kt` — contact row composable; add trailing icon row
- No model or DB changes needed — `emails` and `phoneNumbers` are already on `Contact`

## Key Notes

- `SITApp.PREFS_NAME` + `SITApp.KEY_ONBOARDING_COMPLETE` — SharedPreferences keys for onboarding state
- Room DB is version 2 — any schema changes need a migration
- `ContactGroupCrossRef` handles the many-to-many Contact ↔ Group relationship
- `StringListConverter` serializes `List<String>` for phone numbers, emails, tags
- Android can send SMS silently via `SmsManager` with SEND_SMS permission — iOS cannot
- WorkManager handles tickle notifications — persists across reboots
- `AddContactScreen` doubles as edit screen via optional `contactId` parameter

## Pulse Brand in Compose

```kotlin
val Navy   = Color(0xFF0A1628)   // backgrounds
val Cobalt = Color(0xFF2563EB)   // primary actions
val Amber  = Color(0xFFF5C842)   // accent, tickle due state
```

## Permissions (AndroidManifest)
- `READ_CONTACTS` — contacts import
- `SEND_SMS` — direct SMS (runtime request, graceful fallback to Intent)
- `POST_NOTIFICATIONS` — tickle reminders (Android 13+)

## Build & Run
Open `android/` in Android Studio (Hedgehog or newer).
Run on API 26+ emulator or physical Android device.
SMS direct-send requires physical device with active SIM.

## Sensitive Files — Never Commit
`keystore.jks`, `*.keystore`, `release.properties`, `google-services.json`,
any file containing signing passwords or API keys.
