# CLAUDE.md — Ticklr Android

---

## 🛠️ Pending Tasks — Start Here

Task files contain full diagnosis, root cause, and exact code changes. Read the task file
before making any changes.

| Task file | Type | What it covers |
|---|---|---|
| `tasks/fix-compose-bugs.md` | 🐛 Bug fix | (1) Direct SMS toggle ignored in ComposeScreen — pref read once at ViewModel init, never updated. (2) Contact search field loses focus when DropdownMenu opens — menu steals focus. Scoped to `ui/compose/ComposeScreen.kt` + `ComposeViewModel.kt`. |
| `tasks/feature-tickle-empty-groups-prompt.md` | ✨ Feature | Show empty state + inline "Create a Group" dialog when Group tab has no groups in `TickleEditScreen`. Scoped to `ui/tickle/TickleEditScreen.kt` only. |
| `tasks/feature-unique-group-names.md` | ✨ Feature | Prevent duplicate group names at all creation and rename entry points (GroupListScreen, GroupDetailScreen, ContactDetailScreen, TickleEditScreen). Adds `isGroupNameTaken()` to `GroupViewModel`. |

**Recommended execution order:** fix-compose-bugs → feature-tickle-empty-groups-prompt → feature-unique-group-names (the unique-names task references the dialog added by the empty-groups task).

---

## ✅ Feature: ContactDetailScreen — Add Group Button + Compose Button (complete)

### What to Build

Add two action buttons to `ContactDetailScreen` immediately below the existing
"Add Tickle" button:

---

#### Button 1: Add to Group

**Label:** `"Add to Group"` with `Icons.Default.Group`, Cobalt color
**Behavior:**
- Tapping opens a `ModalBottomSheet` (or `AlertDialog`) listing all existing groups
- Each row: group name with a checkmark `Icons.Default.Check` if contact is already a member
- Tapping a row toggles membership — add if not member, remove if already a member
- At the bottom of the list: a `"+ Create New Group"` row that expands an inline
  `OutlinedTextField` + `"Create"` button
- On create: insert new group, add contact to it, collapse the inline field
- Max 30 char group name, Create button disabled when blank

**ViewModel changes needed in `NetworkViewModel`:**
- `fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>>`
  (already exists in `ContactRepository` — just expose it)
- `fun getAllGroups(): Flow<List<ContactGroup>>`
  (already exists — just expose it in NetworkViewModel or use a shared GroupViewModel)
- `fun addContactToGroup(contactId: Long, groupId: Long)`
- `fun removeContactFromGroup(contactId: Long, groupId: Long)`
- `fun createGroupAndAddContact(groupName: String, contactId: Long)`

**Note:** `ContactRepository` already has `addContactToGroup` and `removeContactFromGroup`.
Expose them via `NetworkViewModel` or inject `GroupViewModel` into the screen.

---

#### Button 2: Message

**Label:** `"Message"` with `Icons.AutoMirrored.Filled.Send`, Cobalt color
**Behavior:**
- Only enabled if the contact has at least one phone number
- On tap: immediately opens SMS intent pre-populated with `contact.phoneNumbers.first`
  and an empty body — bypasses ComposeScreen entirely for speed
- Use `SmsService.sendSmsIntent(context, listOf(phone), "")` which opens the Messages app
- If no phone number: button is greyed out, show `"No phone number on file"` tooltip or
  just rely on the existing empty state already shown in ContactDetailScreen

**No new ViewModel needed** — fire the intent directly from the composable using `LocalContext`.

---

### Button Layout

Replace the current single "Add Tickle" Button with this layout:

```
┌─────────────────────┐  ┌─────────────────────┐
│  🔔 Add Tickle      │  │  👥 Add to Group    │
└─────────────────────┘  └─────────────────────┘
┌──────────────────────────────────────────────┐
│              → Message                        │
└──────────────────────────────────────────────┘
```

Use a `Row` for the first two buttons (equal weight with `Modifier.weight(1f)`),
then a full-width Button for Message below. Tickle stays Amber. Both new buttons use Cobalt.

**Also fix:** `ContactDetailScreen` currently uses Gson `TypeToken` to parse phone/email/tags
— replace with the R8-safe `parseJsonStringArray()` helper (same pattern as ComposeScreen).
This is the same R8 crash risk documented in the critical fix section above.

---

## 🎨 REDESIGN: ComposeScreen — Single Contact + Template Dropdown

### Design Goals
Current `ComposeScreen` is too busy. New design:
- **One contact at a time** — no multi-select, no checkbox list
- **Template dropdown only** — no save/create template on this screen
- **Template CRUD** moves to dedicated screens under Settings
- **Contact search with dropdown results** — no scrollable LazyColumn
- Focused layout that works with keyboard open

### New ComposeScreen Layout (top to bottom)

```
┌────────────────────────────────────────┐
│  Compose                         [TopBar]
├────────────────────────────────────────┤
│  To: [Search contacts field]           │  OutlinedTextField, single line
│      [Selected contact chip + X]       │  shown below field when contact selected
├────────────────────────────────────────┤
│  [Template dropdown — only if          │  OutlinedButton "Select template ▾"
│   templates exist, hidden otherwise]   │  selecting auto-fills message body
├────────────────────────────────────────┤
│  Message                               │
│  ┌──────────────────────────────────┐  │
│  │  OutlinedTextField (min 120dp)   │  │
│  └──────────────────────────────────┘  │
├────────────────────────────────────────┤
│                       [Send button →]  │  disabled until contact + message set
└────────────────────────────────────────┘
```

### Contact Search Behavior
- Typing in "To:" filters contacts by name/company in real time
- Results appear as `DropdownMenu` anchored below field (not a LazyColumn)
- Tapping a result: selects contact, clears search text, shows chip, moves focus to message
- Chip shows full name + X button to clear and re-search
- If selected contact has no phone number: inline warning "No phone number on file"

### Template Dropdown Behavior
- Hidden entirely if `templates.isEmpty()`
- Selecting a template populates `messageBody` — user can still edit after
- Label: "Select template" when none, template title when selected

### Send Button
- Enabled only when: contact selected AND has phone number AND message not blank
- Respects `sendDirectly` pref — SmsManager or Intent
- After send: clear contact + message, show TicklrToast "Message sent ✓"
- **Remove all multi-recipient logic** — single contact only

---

### ComposeViewModel Changes

**Remove:**
- `selectedContactIds: MutableStateFlow<Set<Long>>`
- `toggleContactSelection(contactId: Long)`
- `clearSelection()`
- `SortOrder` enum, `sortOrder` StateFlow, `setSortOrder()`
- `saveTemplate()` — moves to `TemplateViewModel`
- `deleteTemplate()` — moves to `TemplateViewModel`

**Add:**
- `selectedContact: MutableStateFlow<Contact?>` — single selected contact
- `fun selectContact(contact: Contact)` — sets contact, clears searchQuery
- `fun clearContact()` — clears selectedContact
- `fun clearCompose()` — clears selectedContact + messageBody
- `val canSend: StateFlow<Boolean>` — contact != null && phones not empty && message not blank

**Keep:** `searchQuery`, `setSearchQuery()`, `contacts` (filtered, no sort), `templates`,
`messageBody`, `setMessage()`, `applyTemplate()`, `sendDirectly`, `toastMessage`, `clearToast()`

---

### New Template Management Screens (under `ui/settings/`)

#### `TemplateListScreen.kt`
- `LazyColumn` of all templates — title + 2-line body preview
- Swipe to delete (same SwipeToDismiss pattern as TickleListScreen)
- FAB "+" → navigate to `TemplateEditScreen(templateId = -1)`
- Empty state: "No templates yet. Tap + to create one."
- Seeds default "Checking in" template on first launch via SharedPrefs flag
  `hasSeededDefaultTemplates` (mirror iOS behavior)

#### `TemplateEditScreen.kt`
- Two fields: `Title` (single line) + `Body` (multiline min 80dp)
- Save/Update button in TopBar — disabled until both fields non-blank
- Pre-populate when editing existing template
- On save: call ViewModel → popBackStack

#### `TemplateViewModel.kt`
```kotlin
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repo: MessageTemplateRepository
) : ViewModel() {
    val templates: StateFlow<List<MessageTemplate>>
    fun saveTemplate(title: String, body: String)
    fun updateTemplate(template: MessageTemplate)
    fun deleteTemplate(template: MessageTemplate)
    fun seedDefaultIfNeeded(prefs: SharedPreferences)
}
```

---

### Navigation Changes

**`Screen.kt` — add:**
```kotlin
object TemplateList : Screen("template_list")
data class TemplateEdit(val id: Long = -1L) : Screen("template_edit/{templateId}") {
    companion object {
        const val ROUTE = "template_edit/{templateId}"
        fun createRoute(id: Long = -1L) = "template_edit/$id"
    }
}
```

**`NavGraph.kt` — add composables for TemplateList and TemplateEdit**

**`SettingsScreen.kt` — add `onTemplates: () -> Unit` param + SettingsRow in Data section**

---

### Files to Create
- `ui/settings/TemplateListScreen.kt`
- `ui/settings/TemplateEditScreen.kt`
- `ui/settings/TemplateViewModel.kt`

### Files to Modify
- `ui/compose/ComposeScreen.kt` — full redesign
- `ui/compose/ComposeViewModel.kt` — per changes above
- `ui/nav/Screen.kt` — add TemplateList + TemplateEdit
- `ui/nav/NavGraph.kt` — add template composables, update SettingsScreen call
- `ui/settings/SettingsScreen.kt` — add onTemplates param + row

### Files to Leave Alone
- `data/model/MessageTemplate.kt`, `data/dao/MessageTemplateDao.kt`,
  `data/repository/MessageTemplateRepository.kt`, all other screens

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
│   ├── StringListConverter.kt      # Room TypeConverter for List<String> — Gson-free, R8-safe
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
│   │   ├── NetworkListScreen.kt    # Searchable contact list + reachability icons
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
│       ├── TagChipRow.kt
│       └── TicklrToast.kt          # Shared toast overlay (Cobalt bg, white text, 12dp radius)
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
- `NetworkListScreen` — searchable contact list with trailing reachability icons (phone/email)
- `ContactDetailScreen` — full detail view with tickle shortcut
- `AddContactScreen` — create + edit (reused via contactId param)
- `GroupListScreen` + `GroupDetailScreen`
- `GroupDetailScreen` — add-member toast: `"Name added to Group"` / `"Name added to group"` (truncates >20 chars); 30-char group name limit with live counter in both create and edit dialogs
- `TickleListScreen` — Due/Upcoming/Snoozed sections; swipe-to-complete resets row via `dismissState.reset()` in `LaunchedEffect`
- `TickleEditScreen` — create/edit with contact/group picker, frequency, date, note; contact search clears on selection; save/update shows `"Tickle saved"` / `"Tickle updated"` toast
- `TickleScheduler` — nextDueDate logic + WorkManager scheduling
- `TickleWorker` — WorkManager Worker for background notifications
- `ComposeScreen` + `ComposeViewModel` — single-contact redesign; contact search with DropdownMenu chip; template dropdown (hidden if empty); canSend StateFlow; "Message sent ✓" toast after send
- `TemplateListScreen` + `TemplateEditScreen` + `TemplateViewModel` — full template CRUD under Settings; swipe-to-delete; seeds default "Checking in" template on first launch; accessed via "Message Templates" row in Settings
- `ContactDetailScreen` — "Add Tickle" + "Add to Group" buttons in Row, full-width "Message" button below; `ModalBottomSheet` for group membership toggle + inline "Create New Group" (30-char limit); Gson TypeToken replaced with R8-safe `parseJsonStringArray()`
- `SmsService` — SmsManager direct send + Intent fallback
- `ImportScreen` — LinkedIn CSV + contacts import; LinkedIn section framed as relationship seeding with correct copy
- `ContactImportService` — ContactsContract import
- `LinkedInCSVParser` — mirrors iOS implementation
- `SettingsScreen` + `SettingsViewModel` — includes debug "Load Test Contacts" button
- `SeedDataService` — DEBUG only, loads `test_contacts.csv` from assets
- `LaunchScreen` — Pulse identity splash
- `OnboardingScreen`
- `TicklrToast` — shared `Box`-overlay toast composable in `ui/shared/`; used by GroupDetailScreen, ComposeScreen, TickleEditScreen
- `ContactFingerprint` — Gson-free, R8-safe; uses `parseJsonStringArray()` instead of TypeToken
- `StringListConverter` — Gson-free, R8-safe; plain string parser for `fromString`, manual serializer for `fromList`
- Unit tests — LinkedInCSVParser, StringListConverter, TickleScheduler, ScreenRoute, ContactRepository (all passing)
- Build artifacts present — app has been compiled and built successfully
- Screenshot prep — `./gradlew screenshotPrep` sets 9:41, full signal, 100% battery, no notifications
- Screenshot teardown — `./gradlew screenshotTeardown` restores normal status bar

### Android — Nice to Have
- **SmsManager direct send UX** — surface the "send directly vs open Messages" preference in Settings

- **Contact filter in `AddMemberSheet`** — The bottom sheet that opens when adding members to a group (`GroupDetailScreen.kt`, composable `AddMemberSheet` ~line 249) has no search/filter field. Users with large contact lists have to scroll to find someone. iOS already has this via `.searchable()` on its `NavigationStack`. Add a filter text field to Android to match.

  **Implementation — all changes are inside `AddMemberSheet`:**
  1. Add state: `var searchQuery by remember { mutableStateOf("") }`
  2. Derive filtered list: `val filteredContacts = contacts.filter { it.displayName.contains(searchQuery, ignoreCase = true) }`
  3. Insert an `OutlinedTextField` between the "Add Member" title `Text` and the `LazyColumn`:
     ```kotlin
     OutlinedTextField(
         value = searchQuery,
         onValueChange = { searchQuery = it },
         placeholder = { Text("Filter contacts…") },
         leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
         singleLine = true,
         shape = RoundedCornerShape(24.dp),
         modifier = Modifier
             .fillMaxWidth()
             .padding(horizontal = 16.dp, vertical = 8.dp)
     )
     ```
  4. Pass `filteredContacts` instead of `contacts` to the `LazyColumn`'s `items()` call.
  5. `searchQuery` resets naturally when the sheet is dismissed (scoped to composable lifetime) — verify this is true in practice and add an explicit `LaunchedEffect` reset if needed.

  **iOS reference:** `ios/Sources/SIT/Views/Network/GroupDetailView.swift` — `AddMemberSheet` uses `@State private var searchText` filtered against `displayName`, applied via `.searchable()` on the `NavigationStack`. Case-insensitive match on `displayName`.

---

## Key Notes

- `SITApp.PREFS_NAME` + `SITApp.KEY_ONBOARDING_COMPLETE` — SharedPreferences keys for onboarding state
- Room DB is version 2 — any schema changes need a migration
- `ContactGroupCrossRef` handles the many-to-many Contact ↔ Group relationship
- `StringListConverter` serializes `List<String>` for phone numbers, emails, tags — Gson-free as of v1.4.4
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
