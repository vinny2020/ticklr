# CLAUDE.md — Ticklr (Monorepo)

## What This Repo Is

Ticklr is a privacy-first personal network manager with two native platform implementations.
All data is stored on-device. No cloud, no analytics, no account required.

## Repo Structure

```
ticklr/ (local folder: SIT-monorepo)
├── ios/        — Swift 6, SwiftUI, SwiftData (see ios/CLAUDE.md for full iOS spec)
├── android/    — Kotlin, Jetpack Compose, Room (see android/CLAUDE.md for full Android spec)
└── assets/
    ├── brand/  — Shared Pulse logo SVGs and color tokens
    └── seed/   — test_contacts.csv (20 fake contacts for debug seeding)
```

## Always read the platform CLAUDE.md before working

- iOS work → read `ios/CLAUDE.md` first
- Android work → read `android/CLAUDE.md` first

## Shared Brand Tokens

| Token | Value | Usage |
|---|---|---|
| Navy | `#0A1628` | App background, icon bg |
| Cobalt | `#2563EB` | Speech bubble, primary action |
| Amber | `#F5C842` | EKG wave, accent, tickle due state |
| Wordmark | Bebas Neue 400 | "Ticklr" logotype (canonical — do not substitute) |
| Tagline | Inter 400, 10px, 0.16em tracking, amber @ 50% opacity | "YOUR PEOPLE MATTER" — always uppercase |
| Body | Inter 400/500/600/700 | All web/marketing body copy |

> Full token reference, preview HTMLs, UI kits, and brand assets live in `assets/design-system/`. Read `assets/design-system/project/README.md` for the complete spec.

## Working with the Design System

The design system lives in its own repo (`github.com/vinny2020/ticklr-design-system`, private)
and is consumed here as a **git submodule** at `assets/design-system/`. Fresh clones need
`git clone --recurse-submodules ...` (or `git submodule update --init` after a regular clone).

**Before any UI change**, read these in order:
1. `assets/design-system/project/README.md` — voice, color, type, motion, iconography rules.
2. `assets/design-system/project/colors_and_type.css` — the canonical token values.
3. The most relevant `assets/design-system/project/preview/*.html` card OR `ui_kits/ticklr-{ios,android}/components.jsx` snippet for the surface you're touching.

The HTML/JSX is a **reference**, not source of truth — recreate visually in SwiftUI / Jetpack
Compose. The screenshots in `assets/design-system/project/assets/` (e.g.
`network-screen-reference.png`) are pixel ground truth.

**Platform divergence is intentional**, not drift. The system explicitly specs different
treatments per platform — most notably the contact-row avatar (iOS: lavender bg + indigo
text, Android: solid Cobalt + white text). Don't unify without checking the spec first.

**Bundled fonts**: Bebas Neue is bundled in both apps for the wordmark. Use
`Font.custom("BebasNeue-Regular", size:)` on iOS, `FontFamily(Font(R.font.bebas_neue))` on
Android. The shared `WordmarkLockup` component on each platform is the canonical render.

**Updating the pinned design-system version**:
```
cd assets/design-system && git pull origin main && cd -
git add assets/design-system && git commit -m "Bump design-system pin"
```

## App Identity

- **App name**: Ticklr
- **Tagline**: Your People Matter
- **Bundle ID (iOS)**: `com.xaymaca.sit` (unchanged — safe for store submission)
- **Application ID (Android)**: `com.xaymaca.sit` (unchanged — safe for store submission)
- **GitHub repo**: `github.com/vinny2020/ticklr`
- **Landing page**: `xaymaca.com/sit` (subdomain kept as-is)
- **Privacy policy**: `xaymaca.com/sit/privacy`
- **Support URL**: `xaymaca.com/sit/support`

## Feature Parity Status

| Feature | iOS | Android |
|---|---|---|
| Contact import (phone + LinkedIn CSV) | ✅ | ✅ |
| Groups / circles | ✅ | ✅ |
| Tickle calendar (recurring reminders) | ✅ | ✅ |
| Compose SMS/MMS with templates | ✅ | ✅ |
| Settings | ✅ | ✅ |
| Unit tests | — | ✅ (3 suites) |
| App icon (Pulse identity) | ✅ | ✅ Adaptive icon |
| Debug seed / clear contacts | ✅ | ✅ |
| Store-ready | ⚠️ TestFlight pending | ⚠️ Signing pending |

## Debug Tools (gated by DEBUG flag — not in release builds)

Both platforms have a **Debug section** in Settings:
- **Load Test Contacts** — seeds 20 fake contacts from `assets/seed/test_contacts.csv`
- **Clear All Contacts** — wipes all contacts with confirmation (iOS only so far)

iOS: gated by `#if DEBUG` in `SettingsView.swift` via `SeedDataService.swift`
Android: gated by `BuildConfig.DEBUG` in `SettingsViewModel.kt` via `SeedDataService.kt`

## Platform-Specific Notes

**iOS** — `MFMessageComposeViewController` always requires user tap to send. No silent send possible.
**Android** — `SmsManager` can send silently with SEND_SMS permission. Intent fallback also available.

> Android's silent SMS send is a meaningful UX advantage. Surface as a user preference in Settings.

---

## 🚧 Next Feature: Group Member Selection — Add Confirmation Toast

### Problem
When a user picks a person from the contact list to add to a group, the contact disappears from
the list immediately with no feedback. This is confusing — the user doesn't know if the action
succeeded or where the contact went.

### Solution
Show a brief, unobtrusive **toast/snackbar** message after a contact is added to a group.

**Message format:**
- Short group name: `"John User added to Hiking Crew"`
- Long group name (>20 chars): `"John User added to group"` (truncate group name, use generic fallback)

**Behavior:**
- Auto-dismisses after ~2 seconds
- Non-blocking — user can keep selecting contacts
- Appears at the bottom of the screen (above tab bar on iOS, standard snackbar position on Android)
- Uses existing brand colors: Cobalt `#2563EB` background, white text

### Group Name Character Limit
To prevent overflow in toasts, labels, and list rows, enforce a **30-character maximum** on group names.
- Show character count in the group name text field (e.g., `"12 / 30"`)
- Disable save if name is empty or exceeds 30 characters
- Apply on both create and edit flows

### Scope
This feature must be implemented on **both iOS and Android** with matching UX behavior.
See platform CLAUDE.md files for implementation details.
