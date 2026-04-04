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

## App Store Listing Copy

See `docs/app-store-listing.md` for the complete App Store submission copy including:
- App name, subtitle, promotional text
- Full description (copy-paste ready)
- Keywords (under 100 chars)
- Category recommendations
- URLs (support, marketing, privacy policy)
- Screenshot order and tips

## 🚧 Feature: ContactDetailView — Add Group Button + Compose Button

### What to Build

Add two action buttons to `ContactDetailView` immediately below the existing
"Add Tickle Reminder" button:

---

#### Button 1: Add to Group

**Label:** `"Add to Group"` with `person.3.fill` SF Symbol, Cobalt tint
**Behavior:**
- Tapping opens a sheet showing all existing groups in a `List`
- Each row shows the group emoji + name, with a checkmark if the contact is already a member
- Tapping a group row toggles membership (add if not member, remove if already member)
- At the bottom of the list, a `"+ Create New Group"` button opens a simple inline
  `TextField` + `"Create"` button — creates the group and immediately adds the contact to it
- Sheet has a `"Done"` dismiss button in the toolbar
- No navigation push — use a `.sheet` presentation

**New group creation in the sheet:**
- `TextField("Group name…")` + `Button("Create")`
- On create: insert new `ContactGroup` into context, add contact to it, dismiss inline creation
- Max 30 chars on group name (mirror existing GroupDetailView constraint)
- Empty name disables Create button

**Files:** `ContactDetailView.swift` — add `@State private var showingAddToGroup = false`
and the sheet with its own `@Query(sort: \ContactGroup.name) private var allGroups`

---

#### Button 2: Message

**Label:** `"Message"` with `message.fill` SF Symbol, Cobalt tint
**Behavior:**
- Only enabled if `!contact.phoneNumbers.isEmpty`
- If disabled: show button greyed out (no phone number state already visible on detail view)
- On tap: opens `MFMessageComposeViewController` (via existing `MessageComposerService`)
  pre-populated with `contact.phoneNumbers.first` as recipient, empty body
- Uses the same `MessageComposerView` UIViewControllerRepresentable already in the project

**Files:** `ContactDetailView.swift` — add `@State private var showingCompose = false`
and `.sheet(isPresented: $showingCompose) { MessageComposerView(recipients: [phone], body: "") }`

---

### Button Layout

Place both buttons in a single `HStack` replacing the current single-button Section,
so they sit side by side:

```
┌──────────────────┐  ┌──────────────────┐
│  🔔 Add Tickle   │  │  👥 Add to Group │
└──────────────────┘  └──────────────────┘
┌──────────────────────────────────────────┐
│          💬 Message                      │
└──────────────────────────────────────────┘
```

Or use a `VStack` of two rows if HStack feels cramped — use your judgment on device.
Tickle stays Amber. Add to Group and Message use Cobalt.
Message button full-width since it's the primary send action.

---

## 🚧 Next Feature: Group Member Selection — Add Confirmation Toast

### Context
When a user picks a contact from the list inside `GroupDetailView` to add to a group, the contact
disappears from the list with no feedback. This is confusing UX.

### What to Build

**1. Toast notification in `GroupDetailView.swift`**

After a contact is successfully added to a group, show a brief overlay message:
- Format: `"<FirstName> <LastName> added to <GroupName>"` if group name ≤ 20 chars
- Format: `"<FirstName> <LastName> added to group"` if group name > 20 chars
- Auto-dismiss after 2 seconds
- Non-blocking — user can continue selecting contacts while it's visible
- Position: bottom of screen, above tab bar safe area
- Style: Cobalt `#2563EB` background, white text, rounded corners, subtle drop shadow
- Implement as a SwiftUI overlay modifier — no third-party libraries

**2. Character limit on group name in `GroupDetailView.swift` (create + edit flows)**

- Max 30 characters for group name
- Show live character count: `"12 / 30"` displayed below the text field in a muted color
- Disable Save/Create button if name is empty OR exceeds 30 characters
- Apply to both the create-new-group sheet and the inline edit field

### Files Likely Involved
- `Views/Network/GroupDetailView.swift` — primary file; add toast overlay + char limit logic
- Possibly `Views/Network/GroupListView.swift` — if group creation sheet lives there

### Key Constraints
- Swift 6 strict concurrency — use `@MainActor` where needed
- SwiftUI only — no UIKit for the toast
- No third-party dependencies
- Toast must not block interaction with the list underneath it

---

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

### 🔄 UX Copy Update: Set Expectations on LinkedIn Import

**What to change:**
LinkedIn imports only bring in name + company + job title — no phone numbers, rarely an email.
Users may be confused when imported contacts appear "empty." Update the copy in `ImportView`
to frame LinkedIn import as **relationship seeding**, not a full contact sync.

**Suggested language (use as a guide, polish the tone to match the app's voice):**
- Heading or subtitle: `"Seed your network from LinkedIn"`
- Helper text beneath: `"LinkedIn exports include names and companies — no phone numbers.
  You'll add contact details manually after importing."`
- Or as a callout card: `"LinkedIn data gives you the who. You fill in the how to reach them."`

**Where:** `Views/Onboarding/ImportView.swift` — update the LinkedIn section description copy.
Do not change the step-by-step instructions, only the framing copy above or around them.

---

## 🚧 Enhancement: Contact Reachability Icons on Network List

### What to Build
In `NetworkListView` (and `ContactRowView`), show small inline icons next to each contact
indicating how they can be reached:

- 📧 Envelope icon — contact has at least one email address
- 📞 Phone icon — contact has at least one phone number
- Both icons — contact has both
- No icons — contact has neither (LinkedIn-only seed data)

**Design:**
- Use SF Symbols: `envelope` and `phone` (or `envelope.fill` / `phone.fill`)
- Size: `.caption` / 12pt, muted color (e.g. `Color.secondary` or a dimmed Cobalt)
- Position: trailing end of the contact row, before the chevron if one exists
- Must not clutter the row — keep icons small and subtle

**Logic:**
- Email present: `!contact.emails.isEmpty`
- Phone present: `!contact.phoneNumbers.isEmpty`

**Files likely involved:**
- `Views/Network/ContactRowView.swift` — primary; add icon row
- `Views/Network/NetworkListView.swift` — verify rows use `ContactRowView`

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
