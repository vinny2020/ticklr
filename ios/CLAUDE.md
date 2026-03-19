# CLAUDE.md — Ticklr (iOS)

## What This App Is

Ticklr is a privacy-first iOS native app. Users build a curated personal contact network, organize contacts into groups, set recurring tickle reminders to stay in touch, and send SMS/MMS via the native Messages app. All data is stored locally using SwiftData. No cloud, no analytics, no account required.

## Architecture

- **Language**: Swift 6, strict concurrency
- **UI**: SwiftUI throughout — no UIKit views except MessageUI wrapper
- **Persistence**: SwiftData (`@Model` classes in `Models/`)
- **Min target**: iOS 17.0
- **No third-party dependencies** — only Apple frameworks
- **Real-world dataset**: 1,808 contacts imported — optimize for large lists

## Project Structure

```
Sources/SIT/
├── App/
│   ├── SITApp.swift               # @main, ModelContainer, launch → onboarding → main flow
│   ├── ContentView.swift          # Root TabView: Network, Tickle, Groups, Compose, Settings
│   └── LaunchScreenView.swift     # Animated Pulse logo splash (2s fade)
├── Models/
│   ├── Contact.swift              # @Model
│   ├── ContactGroup.swift         # @Model
│   ├── MessageTemplate.swift      # @Model
│   └── TickleReminder.swift       # @Model — includes customIntervalDays: Int?
├── Views/
│   ├── Network/
│   │   ├── NetworkListView.swift  # Searchable (name, company), sorted by lastName
│   │   ├── ContactRowView.swift
│   │   ├── ContactDetailView.swift
│   │   ├── AddContactView.swift
│   │   ├── GroupListView.swift
│   │   └── GroupDetailView.swift
│   ├── Tickle/
│   │   ├── TickleListView.swift   # Sections: Due/Overdue, Upcoming, Snoozed
│   │   ├── TickleRowView.swift
│   │   └── TickleEditView.swift
│   ├── Compose/
│   │   └── ComposeView.swift      # Multi-select + search + template picker
│   ├── Onboarding/
│   │   ├── OnboardingView.swift
│   │   └── ImportView.swift       # iOS Contacts + LinkedIn CSV + step-by-step guide
│   └── Settings/
│       ├── SettingsView.swift     # Contacts count, notifications toggle, default frequency, about
│       ├── TemplateListView.swift # CRUD list — seeds one default template on first launch
│       └── TemplateEditView.swift # Create/edit sheet with title + body fields
├── Services/
│   ├── ContactImportService.swift # CNContactStore bulk import
│   ├── LinkedInCSVParser.swift    # CSV parsing — handles metadata lines, all fields
│   ├── MessageComposerService.swift # MFMessageComposeViewController wrapper
│   └── TickleScheduler.swift      # nextDueDate logic + UNUserNotificationCenter
└── Resources/
    ├── Info.plist
    └── Assets.xcassets/
        └── AppIcon.appiconset/    # All sizes: 20–1024pt, Pulse identity
```


## What's Complete ✅ — Full iOS Feature Set

- `LaunchScreenView` — animated Pulse EKG splash, 2s fade, tagline "YOUR PEOPLE MATTER"
- `SITApp` — launch → onboarding → main flow with animation
- All SwiftData models — Contact, ContactGroup, MessageTemplate, TickleReminder (with customIntervalDays)
- `NetworkListView` — searchable by name/company, sorted lastName, empty states
- `ContactDetailView` — full @Bindable edit form (301 lines)
- `AddContactView` — manual contact creation
- `GroupListView` + `GroupDetailView` — group management
- `ContactImportService` — CNContactStore bulk import with permission handling
- `LinkedInCSVParser` — full CSV parsing, metadata-aware, all fields mapped
- `ImportView` — both paths wired, file picker, error handling, 7-step LinkedIn guide, 10–30 min wait notice
- `TickleListView` — Due/Upcoming/Snoozed sections, swipe actions (complete, snooze, edit, delete)
- `TickleRowView` — avatar, frequency badge, due date, checkmark action
- `TickleEditView` — full create/edit sheet with contact/group picker, frequency, date, note
- `TickleScheduler` — all frequencies, UNUserNotificationCenter, 9am calendar trigger, overdue fallback
- `ComposeView` — multi-select with search (name, company, job title), template picker, send button with count badge
- `MessageComposerService` — MFMessageComposeViewController UIViewControllerRepresentable
- `TemplateListView` — full CRUD, edit button, swipe-to-delete, seeds default template on first launch
- `TemplateEditView` — title + body form, create and edit modes, Save disabled when empty
- `SettingsView` — contacts count, import link, templates link, notification toggle, default tickle frequency picker, app version from bundle, reset onboarding, debug tools
- `ContentView` — 5-tab navigation: Network, Tickle, Groups, Compose, Settings
- `SeedDataService` — DEBUG only, loads `test_contacts.csv` from bundle via `LinkedInCSVParser`
- Debug section in Settings — "Load Test Contacts" + "Clear All Contacts" (both `#if DEBUG` only)
- App icon — single `icon_1024.png` in modern universal format, Pulse identity
- `PRODUCT_NAME` set to `Ticklr` — shows correctly under icon on home screen
- Portrait-only orientation locked via `UISupportedInterfaceOrientations` in `project.yml`

## What's Left

### iOS — App Store Prep
- **Screenshots** — use `Settings → Debug → Clear All Contacts` then `Load Test Contacts` for clean 20-contact dataset, then take screenshots on Pixel 7 Pro emulator equivalent
- **TestFlight** — enroll at developer.apple.com ($99/yr), archive build, distribute internally first
- **App Store listing** — screenshots (iPhone 6.9" required), description, keywords
- **Privacy policy URL** — already live at `xaymaca.com/sit/privacy` ✅
- **Support URL** — already live at `xaymaca.com/sit/support` ✅


## Key Conventions

- All SwiftData models use `@Model` — never CoreData
- Views receive `modelContext` via `@Environment(\.modelContext)`
- Services are structs with static methods — no singletons
- Never call network APIs — fully offline
- `MFMessageComposeViewController` only works on real device — not Simulator
- Optimize all lists for 1,800+ contacts — use `@Query` with sort descriptors
- `@AppStorage("hasSeededDefaultTemplates")` guards one-time default template seeding
- `@AppStorage("tickleNotificationsEnabled")` + `@AppStorage("defaultTickleFrequency")` persist user prefs
- Notification permission is requested lazily on first tickle creation or when toggled in Settings

## Brand

- **Background**: Navy `#0A1628`
- **Primary / bubble**: Cobalt `#2563EB`
- **Accent / tickle due**: Amber `#F5C842` — `Color(red: 0.96, green: 0.78, blue: 0.25)`
- **Tab tint**: `.indigo`
- **Wordmark**: Syne 800 — "Ticklr" / "YOUR PEOPLE MATTER"

## LinkedIn Import Notes

- Export takes 10–30 min — surfaced in ImportView ✅
- Full flow works on iPhone in Safari — no desktop required ✅
- LinkedIn never includes phone numbers — user must add manually
- Emails only present if connection made them visible
- `LinkedInStep` is the reusable numbered-step component in ImportView

## Build & Run

```bash
cd ios
xcodegen generate   # required after ANY project.yml change or new file added
open SIT.xcodeproj
```

After `xcodegen generate`, re-select signing team:
Xcode → SIT target → Signing & Capabilities → Team → Vincent Stoessel (Personal Team)

**IMPORTANT**: Always run `xcodegen generate` after:
- Adding new Swift or resource files
- Changing `project.yml`
- Pulling changes that include new files

Real device required for: `MFMessageComposeViewController` (SMS compose)
Simulator fine for: everything else including tickle notifications, debug seeding

## Sensitive Files — Never Commit
`*.mobileprovision`, `*.p12`, `*.p8`, any file with API keys or Team IDs
