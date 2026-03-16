# CLAUDE.md — SIT Android

## What This Is
Native Android implementation of SIT (Stay in Touch). Kotlin, Jetpack Compose, Room database.
Feature parity with the iOS app. See root `CLAUDE.md` for shared brand tokens.

## Architecture — MVVM + Repository
- **UI**: Jetpack Compose (no XML layouts)
- **State**: ViewModel + StateFlow
- **Persistence**: Room (equivalent to iOS SwiftData)
- **DI**: Hilt
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Project Structure

```
app/src/main/java/com/xaymaca/sit/
├── data/
│   ├── model/          # Room @Entity classes
│   │   ├── Contact.kt
│   │   ├── ContactGroup.kt
│   │   ├── MessageTemplate.kt
│   │   └── TickleReminder.kt
│   └── repository/     # Repository classes (data access layer)
│       ├── ContactRepository.kt
│       └── TickleRepository.kt
├── service/
│   ├── ContactImportService.kt   # ContactsContract import
│   ├── LinkedInCSVParser.kt      # CSV parsing
│   ├── SmsService.kt             # SmsManager direct send OR Intent fallback
│   └── TickleScheduler.kt        # WorkManager recurring reminders
├── ui/
│   ├── theme/          # Compose theme (Pulse colors, typography)
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── network/        # Contact list + detail screens
│   ├── tickle/         # Tickle list + edit screens
│   ├── compose/        # Message compose screen
│   ├── onboarding/     # First-run import flow
│   └── settings/       # Settings screen
└── SITApp.kt           # @HiltAndroidApp Application class
```

## Room Models (mirror iOS SwiftData models)

### Contact
`id: Long`, `firstName`, `lastName`, `phoneNumbers: String (JSON)`,
`emails: String (JSON)`, `company`, `jobTitle`, `notes`,
`tags: String (JSON)`, `importSource: String`, `createdAt: Long`, `lastContactedAt: Long?`

### ContactGroup
`id: Long`, `name`, `emoji`

### TickleReminder
`id: Long`, `contactId: Long?`, `groupId: Long?`, `note`,
`frequency: String`, `startDate: Long`, `nextDueDate: Long`,
`lastCompletedDate: Long?`, `status: String`

## Key Android vs iOS Differences

| Feature | iOS | Android |
|---|---|---|
| Messaging | MFMessageComposeViewController (user taps Send) | SmsManager (can send silently with SEND_SMS permission) OR Intent fallback |
| Contacts import | CNContactStore | ContactsContract |
| Local notifications | UNUserNotificationCenter | WorkManager + NotificationManager |
| Persistence | SwiftData | Room |
| DI | None (structs) | Hilt |

## Android SMS Advantage
Unlike iOS, Android can send SMS silently via `SmsManager` if SEND_SMS permission granted.
Implement a user preference: "Send directly" vs "Open Messages app".
This is a meaningful UX win over iOS.

## Pulse Brand in Compose

```kotlin
val Navy = Color(0xFF0A1628)
val Cobalt = Color(0xFF2563EB)
val Amber = Color(0xFFF5C842)
```

## Permissions Required (AndroidManifest)
- `READ_CONTACTS` — contacts import
- `SEND_SMS` — direct SMS (request at runtime, graceful fallback to Intent)
- `RECEIVE_SMS` — optional, for read receipts
- `POST_NOTIFICATIONS` — tickle reminders (Android 13+)

## What Claude Code Should Build

1. Room database setup (SITDatabase.kt, DAOs)
2. Compose theme (Color.kt, Theme.kt, Type.kt) using Pulse identity
3. Main scaffold (NavHost with bottom nav: Network, Tickle, Compose, Settings)
4. NetworkListScreen + ContactDetailScreen
5. TickleListScreen + TickleEditScreen
6. ComposeScreen (SmsService integration)
7. ContactImportService (ContactsContract)
8. LinkedInCSVParser (identical logic to iOS version)
9. TickleScheduler (WorkManager)
10. OnboardingScreen

## Build & Run
Open `android/` folder in Android Studio (Hedgehog or newer).
Run on physical device or API 26+ emulator.
