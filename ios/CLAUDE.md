# CLAUDE.md — SIT: Stay in Touch (iOS)

## What This App Is

SIT (Stay in Touch) is a privacy-first iOS native app. Users build a curated personal contact network, organize contacts into groups, set recurring tickle reminders to stay in touch, and send SMS/MMS via the native Messages app. All data is stored locally using SwiftData. No cloud, no analytics, no account required.

## Architecture

- **Language**: Swift 6, strict concurrency
- **UI**: SwiftUI throughout — no UIKit views except MessageUI wrapper
- **Persistence**: SwiftData (`@Model` classes in `Models/`)
- **Min target**: iOS 17.0
- **No third-party dependencies** — only Apple frameworks
- **Real-world dataset**: 1,808 contacts imported — optimize for large lists, not small ones

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
│   └── TickleReminder.swift       # @Model
├── Views/
│   ├── Network/
│   │   ├── NetworkListView.swift  # Searchable list, sort by lastName
│   │   ├── ContactRowView.swift
│   │   ├── ContactDetailView.swift # Full edit form (@Bindable)
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
│   │   └── ImportView.swift       # iOS Contacts + LinkedIn CSV — both fully wired
│   └── Settings/
│       └── SettingsView.swift
├── Services/
│   ├── ContactImportService.swift # CNContactStore bulk import — implemented
│   ├── LinkedInCSVParser.swift    # CSV parsing — implemented
│   ├── MessageComposerService.swift # MFMessageComposeVC wrapper — implemented
│   └── TickleScheduler.swift      # UNUserNotificationCenter — implemented
└── Resources/
    ├── Info.plist
    └── Assets.xcassets/
```


## What's Complete ✅

- `LaunchScreenView` — animated Pulse EKG splash, 2s fade
- `SITApp` — launch → onboarding → main tab flow
- All SwiftData models — Contact, ContactGroup, MessageTemplate, TickleReminder
- `NetworkListView` — searchable (name, company), sort by lastName, empty states
- `ContactDetailView` — full @Bindable edit form
- `AddContactView` — manual contact creation
- `GroupListView` + `GroupDetailView` — group management
- `ContactImportService` — CNContactStore bulk import with permission handling
- `LinkedInCSVParser` — full CSV parsing, handles metadata lines, all fields mapped
- `ImportView` — both import paths wired, file picker, error handling, step-by-step LinkedIn guide
- `TickleListView` — Due/Upcoming/Snoozed sections, swipe actions (complete, snooze, edit, delete)
- `TickleRowView` — avatar, frequency badge, due date, checkmark
- `TickleEditView` — full create/edit sheet
- `TickleScheduler` — nextDueDate logic for all frequencies, UNUserNotificationCenter integration
- `ComposeView` — multi-select contacts with search (name, company, job title), template picker, send button with count badge
- `MessageComposerService` — MFMessageComposeViewController UIViewControllerRepresentable
- `ContentView` — 5-tab navigation (Network, Tickle, Groups, Compose, Settings)
- Search on both Network and Compose screens

## What's Left to Build

### Priority 1 — Message Templates CRUD (High)
`SettingsView` needs a Templates section so users can create/edit/delete templates.
Currently `ComposeView` has a template picker but there's no way to create templates.

**Build:**
- `TemplateListView` — list of templates with add/edit/delete
- `TemplateEditView` — sheet with title (TextField) and body (TextEditor) fields
- Wire into `SettingsView` as a `NavigationLink("Message Templates")`
- Add a default starter template on first launch (e.g. "Checking in — Hey, just wanted to check in!")

### Priority 2 — App Icon (High)
`Assets.xcassets` has no AppIcon set yet — app shows default icon on device.

**Pulse identity:**
- Background: Navy `#0A1628`
- Bubble: Cobalt `#2563EB`
- EKG wave: Amber `#F5C842`
- Required sizes: 1024x1024 (App Store), 60pt @2x/@3x, 40pt @2x/@3x, 29pt @2x/@3x, 20pt @2x/@3x

### Priority 3 — Settings Expansion (Medium)
`SettingsView` is minimal. Expand with:
- Notification preferences (enable/disable tickle notifications)
- Default tickle frequency preference
- Contact count display ("1,808 contacts")
- App version from bundle

### Priority 4 — Android (When Ready)
See `android/CLAUDE.md` — full spec there.
Start with: Room database setup, Compose theme (Pulse colors), NavHost scaffold.


## Key Conventions

- All SwiftData models use `@Model` macro — never CoreData
- Views receive `modelContext` via `@Environment(\.modelContext)`
- Services are structs with static methods — no singletons
- Never call network APIs — fully offline
- `MFMessageComposeViewController` only works on real device — not Simulator
- Optimize all lists for 1,800+ contacts — use `@Query` with sort descriptors, avoid in-memory filtering of the full set where possible

## Brand

- **Background**: Navy `#0A1628`
- **Primary action**: Cobalt `#2563EB`
- **Accent / tickle due**: Amber `#F5C842` (`Color(red: 0.96, green: 0.78, blue: 0.25)`)
- **Tab tint**: `.indigo`
- **Wordmark**: Syne 800 — "SIT" / "STAY IN TOUCH"

## LinkedIn Import Notes

- Export takes 10–30 min — surfaced in ImportView UI ✅
- Entire flow works on iPhone in Safari — no desktop required ✅
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

Target device: physical iPhone — `MFMessageComposeViewController` requires real device.
Simulator is fine for all other features including tickle notifications.

## Sensitive Files — Never Commit
`*.mobileprovision`, `*.p12`, `*.p8`, any file with API keys or Team IDs
