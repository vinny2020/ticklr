# CLAUDE.md — Ticklr (iOS)

> **Status:** Live on the App Store (April 2026). The historical record of completed work — i18n phases, store submission cycles, feature builds, bug fixes — lives in `~/Documents/SecondBrain/Projects/Ticklr/CLAUDE Archive — Shipped Work.md`. This file tracks reference material and ongoing work only.

---

## 🛠️ Pending Tasks — Start Here

> No pending iOS tasks. See the archive for shipped work.

---

## 🔮 Future Considerations

> Not on the runway. Reference notes for if these ever come up.

### RTL (right-to-left) language support

Only needed if Arabic, Hebrew, Urdu, Farsi, or other RTL languages are added to the String
Catalog. SwiftUI handles RTL automatically for most components (`HStack` reverses,
`NavigationStack` flips back buttons, `.padding(.leading)` maps to right side), but a few
items need attention:

- Audit SF Symbols — directional symbols (chevrons, send icons) may need
  `.flipsForRightToLeftLayoutDirection(true)`; symbols like `checkmark`, `bell`, `plus`,
  `trash`, `pencil`, and the brand logo should NOT mirror.
- Fix any hardcoded `x` offsets in `.offset()` or `.position()` (these don't auto-flip).
- The launch screen Pulse logo and "YOUR PEOPLE MATTER" tagline should remain centered;
  the tagline stays English.
- Arabic has complex plural rules (zero/one/two/few/many/other) — String Catalog supports all six.
- Arabic text is wider than English in many fonts — test for truncation.
- `Text(verbatim:)` doesn't flip — correct for phone numbers, URLs, code.

This is the most labor-intensive phase if undertaken. Treat as a project unto itself.

---

## What This App Is

Ticklr is a privacy-first iOS native app. Users build a curated personal contact network,
organize contacts into groups, set recurring tickle reminders to stay in touch, and send
SMS/MMS via the native Messages app. All data stored locally using SwiftData. No cloud,
no analytics, no account required.

## Architecture

- **Language**: Swift 6, strict concurrency
- **UI**: SwiftUI throughout — no UIKit views except MessageUI wrapper
- **Persistence**: SwiftData (`@Model` classes in `Models/`)
- **Min target**: iOS 17.0
- **Localization**: String Catalog (`Localizable.xcstrings`); shipped languages: English, Spanish
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
│   ├── Network/        # NetworkListView, ContactRowView, ContactDetailView,
│   │                   # AddContactView, GroupListView, GroupDetailView
│   ├── Tickle/         # TickleListView, TickleRowView, TickleEditView
│   ├── Compose/        # ComposeView (multi-select + template picker)
│   ├── Onboarding/     # OnboardingView, ImportView (Contacts + LinkedIn CSV)
│   └── Settings/       # SettingsView, TemplateListView, TemplateEditView
├── Services/
│   ├── ContactImportService.swift   # CNContactStore bulk import
│   ├── LinkedInCSVParser.swift      # CSV parsing — handles metadata lines, all fields
│   ├── MessageComposerService.swift # MFMessageComposeViewController wrapper
│   ├── MessageTemplateSeed.swift    # one-time default-template seed run from SITApp.init()
│   └── TickleScheduler.swift        # nextDueDate logic + UNUserNotificationCenter
├── Resources/
│   ├── Info.plist
│   ├── Localizable.xcstrings        # String Catalog — en, es
│   └── Assets.xcassets/
│       └── AppIcon.appiconset/      # All sizes 20–1024pt, Pulse identity
└── Tests/SITTests/
    └── LocalizationTests.swift      # String Catalog coverage tests
```

## Key Conventions

- All SwiftData models use `@Model` — never CoreData
- Views receive `modelContext` via `@Environment(\.modelContext)`
- Services are structs with static methods — no singletons
- Never call network APIs — fully offline
- `MFMessageComposeViewController` only works on real device — not Simulator
- Optimize all lists for 1,800+ contacts — use `@Query` with sort descriptors
- `UserDefaults["hasSeededDefaultTemplates"]` guards one-time default-template seeding, run from `SITApp.init()` via `MessageTemplateSeed.seedIfNeeded(container:)`
- `@AppStorage("tickleNotificationsEnabled")` + `@AppStorage("defaultTickleFrequency")` persist user prefs
- Notification permission is requested lazily on first tickle creation or when toggled in Settings
- Every user-visible string goes through `String(localized:)` keys, never hardcoded
- Brand strings ("Ticklr", "Xaymaca", "YOUR PEOPLE MATTER") are kept untranslated across all locales

## Brand

- **Background**: Navy `#0A1628`
- **Primary / bubble**: Cobalt `#2563EB`
- **Accent / tickle due**: Amber `#F5C842` — `Color(red: 0.96, green: 0.78, blue: 0.25)`
- **Tab tint**: `.indigo`
- **Wordmark**: Bebas Neue (bundled font) — "Ticklr" / "YOUR PEOPLE MATTER"

## App Store Listing Copy

See `docs/app-store-listing.md` for the complete App Store submission copy: app name,
subtitle, promotional text, full description, keywords, category recommendations, and
screenshot order.

## Build & Run

```bash
cd ios
xcodegen generate   # required after ANY project.yml change or new file added
open SIT.xcodeproj
```

After `xcodegen generate`, re-select signing team in Xcode → SIT target → Signing & Capabilities → Team.

**IMPORTANT**: Always run `xcodegen generate` after:
- Adding new Swift or resource files
- Changing `project.yml`
- Pulling changes that include new files

Real device required for: `MFMessageComposeViewController` (SMS compose).
Simulator fine for: everything else including tickle notifications, debug seeding.

## Sensitive Files — Never Commit
`*.mobileprovision`, `*.p12`, `*.p8`, any file with API keys or Team IDs.
