# CLAUDE.md — SIT: Stay in Touch (iOS)

## What This App Is

SIT (Stay in Touch) is a privacy-first iOS native app. Users build a curated personal contact network, organize contacts into groups, set recurring tickle reminders to stay in touch, and send SMS/MMS via the native Messages app. All data is stored locally using SwiftData. No cloud, no analytics, no account required.

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

- `LaunchScreenView` — animated Pulse EKG splash, 2s fade
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
- `SettingsView` — contacts count, import link, templates link, notification toggle (with system permission awareness), default tickle frequency picker, app version from bundle, reset onboarding
- `ContentView` — 5-tab navigation: Network, Tickle, Groups, Compose, Settings
- Search on both Network and Compose screens
- `AppIcon.appiconset` — all required sizes (20–1024pt), Pulse identity

## What's Left

### iOS — App Store Prep
- **TestFlight** — enroll at developer.apple.com ($99), archive build, distribute
- **App Store listing** — screenshots, description, keywords, privacy policy URL
- **Privacy policy** — required for App Store; must document local-only data storage

### Android
See `android/CLAUDE.md` — full spec there.
Suggested first session prompt:
> "Read android/CLAUDE.md then build items 1–3: Room database setup, Compose theme with Pulse colors, and main NavHost scaffold"


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
- **Wordmark**: Syne 800 — "SIT" / "STAY IN TOUCH"

## LinkedIn Import Notes

- Export takes 10–30 min — surfaced in ImportView ✅
- Full flow works on iPhone in Safari — no desktop required ✅
- LinkedIn never includes phone numbers — user must add manually
- Emails only present if connection made them visible
- `LinkedInStep` is the reusable numbered-step component in ImportView

## Build & Run

```bash
cd ios
xcodegen generate   # only needed after project.yml changes
open SIT.xcodeproj
```

After `xcodegen generate`, re-select signing team:
Xcode → SIT target → Signing & Capabilities → Team → Vincent Stoessel (Personal Team)

Real device required for: `MFMessageComposeViewController` (SMS compose)
Simulator fine for: everything else including tickle notifications

## Sensitive Files — Never Commit
`*.mobileprovision`, `*.p12`, `*.p8`, any file with API keys or Team IDs
