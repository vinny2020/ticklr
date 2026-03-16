# CLAUDE.md — SIT: Stay in Touch

## What This App Is

SIT (Stay in Touch) is a privacy-first iOS native app. Users build a curated personal contact network, organize contacts into groups, and send SMS/MMS via the native Messages app. A tickle calendar lets users set recurring reminders to reach out to specific contacts or groups on a schedule. All data is stored locally using SwiftData. No cloud, no analytics, no account required.

## Architecture

- **Language**: Swift 6, strict concurrency
- **UI**: SwiftUI throughout — no UIKit views except MessageUI wrapper
- **Persistence**: SwiftData (`@Model` classes in `Models/`)
- **Min target**: iOS 17.0
- **No third-party dependencies** — only Apple frameworks

## Project Structure

```
Sources/SIT/
├── App/
│   ├── SITApp.swift               # @main, ModelContainer, launch → onboarding → main flow
│   ├── ContentView.swift          # Root TabView (Network, Tickle, Compose, Settings)
│   └── LaunchScreenView.swift     # Animated Pulse logo splash (2s, then fades)
├── Models/
│   ├── Contact.swift              # @Model — core contact entity
│   ├── ContactGroup.swift         # @Model — group/circle of contacts
│   ├── MessageTemplate.swift      # @Model — reusable message templates
│   └── TickleReminder.swift       # @Model — recurring reach-out reminder
├── Views/
│   ├── Network/
│   │   ├── NetworkListView.swift
│   │   ├── ContactRowView.swift
│   │   └── ContactDetailView.swift
│   ├── Tickle/
│   │   ├── TickleListView.swift   # TODO: main tickle dashboard
│   │   ├── TickleRowView.swift    # TODO: single reminder row with due status
│   │   └── TickleEditView.swift   # TODO: create/edit a reminder
│   ├── Compose/
│   │   └── ComposeView.swift
│   ├── Onboarding/
│   │   ├── OnboardingView.swift
│   │   └── ImportView.swift
│   └── Settings/
│       └── SettingsView.swift
├── Services/
│   ├── ContactImportService.swift
│   ├── LinkedInCSVParser.swift
│   ├── MessageComposerService.swift
│   └── TickleScheduler.swift      # TODO: next-date calc + notification scheduling
└── Resources/
    ├── Info.plist
    └── Assets.xcassets/
```

## SwiftData Models (Summary)

### Contact
`id`, `firstName`, `lastName`, `phoneNumbers:[String]`, `emails:[String]`, `company`, `jobTitle`, `notes`, `tags:[String]`, `groups:[ContactGroup]`, `importSource: ImportSource`, `createdAt`, `lastContactedAt`

### ContactGroup
`id`, `name`, `emoji`, `contacts:[Contact]`

### MessageTemplate
`id`, `title`, `body`, `createdAt`

### TickleReminder ← NEW
`id`, `contact: Contact?`, `group: ContactGroup?`, `note`, `frequency: TickleFrequency`, `startDate`, `nextDueDate`, `lastCompletedDate`, `status: TickleStatus`, `createdAt`

**TickleFrequency** enum: `.daily`, `.weekly`, `.biweekly`, `.monthly`, `.bimonthly`, `.quarterly`, `.custom`

**TickleStatus** enum: `.active`, `.snoozed`, `.completed`

## Key Conventions

- All SwiftData models use `@Model` macro — never CoreData
- Views receive `modelContext` via `@Environment(\.modelContext)`
- Services are structs with static methods — no singletons
- `MessageComposerService` wraps `MFMessageComposeViewController` via `UIViewControllerRepresentable`
- Never call network APIs — fully offline
- Use `UNUserNotificationCenter` for tickle local notifications — no push required

## Brand / Design

- **Identity**: Pulse logo — navy background (#0A1628), blue bubble (#2563EB), amber EKG wave (#F5C842)
- **Wordmark**: Syne 800 weight, "SIT" + tracking-wide "STAY IN TOUCH" subtitle
- **Accent color**: amber `#F5C842` for tickle due states, CTAs
- **Launch screen**: `LaunchScreenView.swift` — animated EKG wave, 2s display, fades to app
- **Tab tint**: `.indigo`

## Tickle Feature — Full Spec

### User stories
- "Remind me to call my father every Sunday"
- "Check in with my colleague every 2 months about opportunities"
- "Ping my college friends group every quarter"

### TickleListView
- Sections: **Due today / overdue** (amber accent), **Upcoming** (normal), **Snoozed** (muted)
- Each row: contact avatar + name, frequency badge, next due date, checkmark action
- Tapping checkmark → marks complete, advances `nextDueDate` by frequency interval
- Swipe actions: Snooze (1 week), Edit, Delete
- Empty state: "No tickles yet — add one from a contact's detail page"

### TickleEditView (sheet)
- Pick contact OR group (not both)
- Set frequency (Picker from TickleFrequency)
- Set start date (DatePicker)
- Optional note ("Ask about the new role", "Wish happy birthday")
- Save → creates TickleReminder, schedules local notification

### TickleScheduler (service)
```swift
struct TickleScheduler {
    static func scheduleNotification(for reminder: TickleReminder)
    static func cancelNotification(for reminder: TickleReminder)
    static func markComplete(reminder: TickleReminder, context: ModelContext)
    static func nextDueDate(from date: Date, frequency: TickleFrequency) -> Date
    static func snooze(reminder: TickleReminder, days: Int, context: ModelContext)
}
```

### nextDueDate logic
- `.daily` → +1 day
- `.weekly` → +7 days
- `.biweekly` → +14 days
- `.monthly` → +1 month (Calendar.current.date(byAdding:))
- `.bimonthly` → +2 months
- `.quarterly` → +3 months
- `.custom` → user-defined interval in days (add `customIntervalDays: Int?` field)

### Local notifications
- Use `UNUserNotificationCenter`
- Request `.alert + .sound` permission on first tickle creation
- Notification title: "Time to reach out to [Name]"
- Notification body: reminder.note (if set) else frequency string
- Identifier: `"tickle-\(reminder.id.uuidString)"`
- Trigger: `UNCalendarNotificationTrigger` at 9am on `nextDueDate`

### ContentView tab addition
Add a **Tickle** tab between Network and Compose:
```swift
TickleListView()
    .tabItem { Label("Tickle", systemImage: "bell.badge.clock") }
```

## What's Already Built

- `LaunchScreenView` — animated Pulse logo splash ✅
- `SITApp` — launch → onboarding → main flow ✅
- `TickleReminder` model stub ✅
- All other views as stubs ✅
- `ContactImportService`, `LinkedInCSVParser`, `MessageComposerService` — signatures ready ✅

## What Claude Code Should Build Next

**Priority 1 — Tickle feature (new)**
1. `TickleListView` — sectioned list (due/upcoming/snoozed) with swipe actions
2. `TickleRowView` — row with avatar, frequency badge, due date, checkmark
3. `TickleEditView` — sheet for creating/editing a reminder
4. `TickleScheduler` — nextDueDate logic + UNUserNotificationCenter integration
5. Add Tickle tab to `ContentView`
6. Add "Add Tickle" button to `ContactDetailView`

**Priority 2 — Core features**
7. `ContactImportService` — implement CNContactStore bulk fetch
8. `LinkedInCSVParser` — implement CSV parsing wiring in ImportView
9. `ContactDetailView` — full @Bindable edit form
10. `MessageComposerService` — wire into ComposeView
11. `MessageTemplate` CRUD in SettingsView

## Build & Run

```bash
xcodegen generate
open SIT.xcodeproj
```
Target: physical iPhone — MFMessageComposeViewController and UNUserNotificationCenter require real device.

## Sensitive Files — Never Commit
`*.mobileprovision`, `*.p12`, `*.p8`, any file with API keys or Team IDs
