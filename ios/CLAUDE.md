# CLAUDE.md — Ticklr (iOS)

> **Status:** Live on the App Store (April 2026). The historical record of completed work — i18n phases, store submission cycles, feature builds, bug fixes — lives in `~/Documents/SecondBrain/Projects/Ticklr/CLAUDE Archive — Shipped Work.md`. This file tracks reference material and ongoing work only.

---

## 🛠️ Pending Tasks — Start Here

### Task — Move default-template seeding to app launch

**Bug:** The default "Checking in" `MessageTemplate` is only seeded when the user opens
**Settings → Message Templates** (`TemplateListView.swift` — `.onAppear(perform: seedDefaultIfNeeded)`).
If a user never visits that screen, no template ever exists, and the template picker on
`ComposeView` stays unusable. Same latent bug on Android — see `android/CLAUDE.md`.

**Real fix:** Seed once at app launch.
1. In `SITApp.swift` (`@main` `App` struct), add a one-time seeding step. Two reasonable
   places: inside `init()` after the `ModelContainer` is constructed (open a `ModelContext`,
   run the seed, save), or via `.task` on the root `WindowGroup` view, gated by an
   `@AppStorage` flag.
2. Use the existing `@AppStorage("hasSeededDefaultTemplates")` flag — if `false`, insert the
   default `MessageTemplate(title: "Checking in", body: "Hey! Just checking in — hope you're
   doing well. Let's catch up soon!")` and flip the flag to `true`.
3. **Remove** the `.onAppear(perform: seedDefaultIfNeeded)` from `TemplateListView.swift`
   (and the function itself) once the launch-time seed is in place. Or keep it as a
   defensive safety net — both are defensible.
4. Bonus: extract the seed title + body into a `MessageTemplateSeed` enum or static so the
   launch path and any future defensive paths share one source of truth.

**Why launch-time:** the picker should work on first install regardless of which tab the
user opens first.

**Scope:** `SITApp.swift` (~10 lines), `TemplateListView.swift` (remove seed function),
optionally a new `MessageTemplateSeed.swift` (new helper).

---

### Task — Remove group tickles (parity with Android)

**Why:** Group tickles are conceptually awkward in the relationship-management framing.
A "tickle" is a gentle nudge to reach out to *one person*. When the reminder fires for a
group of 8, the user isn't reaching out to "the group" as a unit — they're reaching out
to 8 individuals on the same cadence, which collapses to either a mass message (rude) or
8 individual nudges (which the user can already do by setting individual tickles). Android
removed this feature already; iOS should match.

**Reference implementation:** Android stripped this out cleanly. See:
- `android/app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt` — comments at
  lines 49 and 67–69 document the removal; `groupId = null` hardcoded in `TickleReminder`
  construction at line 125.
- `android/app/src/main/java/com/xaymaca/sit/ui/tickle/TickleViewModel.kt:67–68` — read
  path defensively renders legacy group tickles' avatars (first letter of group name) so
  pre-removal user data doesn't crash or render blank.
- `android/app/src/main/java/com/xaymaca/sit/data/model/TickleReminder.kt:10` — schema
  field `groupId: Long?` retained for backward compatibility.

**Apply the same pattern on iOS in `Views/Tickle/TickleEditView.swift`:**
1. Remove the segmented `Picker(targetType)` with `.contact`/`.group` cases (around line 59).
2. Remove the entire group branch — the empty state with "Create Group" button, the
   `Picker` listing all groups, and any related conditional rendering (lines ~83–105).
3. Remove `@State private var selectedGroup: ContactGroup?`, `showingCreateGroupSheet`,
   and the `TargetType` enum.
4. Drop the `.sheet(isPresented: $showingCreateGroupSheet) { GroupEditSheet(group: nil) }`
   presentation (lines ~142–143). `GroupEditSheet` itself stays — `GroupListView` still uses it.
5. Drop the `@Query(sort: \ContactGroup.name) private var allGroups` if not used elsewhere
   in the file.
6. **Lock save logic to contact-only** (lines ~170 and ~181): always set `r.group = nil`
   and remove the `targetType == .group ? selectedGroup : nil` branches.

**Keep these — backward compatibility:**
1. **`TickleReminder.group` SwiftData relationship** — leave the field on the model. Users
   with legacy group tickles in their on-device store shouldn't crash on upgrade.
2. **Read path in `TickleListView` / `TickleRowView`** — audit whether they read
   `reminder.group` for avatar/label rendering. If they do, keep that defensive code so
   legacy group tickles still render. (Mirror the Android `reminderInitials` pattern that
   shows the group name's first letter when `groupId != null`.)

**Leave untouched:**
- `Views/Network/GroupListView.swift`, `GroupDetailView.swift`, `GroupEditSheet`
- `ContactDetailView`'s "Add to Group" button and modal sheet
- Group membership data model (`ContactGroup`, `Contact.groups` relationship)

These are about contact-to-group **membership**, which is a different feature and stays.

**Scope:** `Views/Tickle/TickleEditView.swift` (most of the change), audit
`TickleListView.swift` / `TickleRowView.swift` for legacy display path. No model migration,
no schema change.

---

### Task — RTL (right-to-left) language support (optional / future)

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
- `@AppStorage("hasSeededDefaultTemplates")` guards one-time default template seeding (see Pending Task above)
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
