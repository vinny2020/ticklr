# CLAUDE.md — Ticklr Android



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
