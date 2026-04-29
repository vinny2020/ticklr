# CLAUDE.md — Ticklr (Monorepo)

## What This Repo Is

Ticklr is a privacy-first personal network manager. Two native platform implementations.
All data stored on-device. No cloud, no analytics, no account required.

**Both platforms live:** iOS App Store + Google Play Store (April 2026).

## Repo Structure

```
ticklr/
├── ios/        — Swift 6, SwiftUI, SwiftData (read ios/CLAUDE.md first for iOS work)
├── android/    — Kotlin, Jetpack Compose, Room (read android/CLAUDE.md first for Android work)
└── assets/
    ├── design-system/   — git submodule (private repo)
    ├── brand/  — shared logo SVGs + color tokens
    └── seed/   — test_contacts.csv (20 fake contacts for debug seeding)
```

> **Historical record of completed work:** see `~/Documents/SecondBrain/Projects/Ticklr/CLAUDE Archive — Shipped Work.md` for everything that's already shipped (i18n phases, redesigns, bug fixes, store submissions). This file only tracks reference material and ongoing work.

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

Lives in its own repo (`github.com/vinny2020/ticklr-design-system`, private), consumed here as a **git submodule** at `assets/design-system/`. Fresh clones need `git clone --recurse-submodules ...` (or `git submodule update --init` after a regular clone).

**Before any UI change**, read these in order:
1. `assets/design-system/project/README.md` — voice, color, type, motion, iconography rules
2. `assets/design-system/project/colors_and_type.css` — canonical token values
3. The most relevant `assets/design-system/project/preview/*.html` card OR `ui_kits/ticklr-{ios,android}/components.jsx` snippet for the surface you're touching

The HTML/JSX is a **reference**, not source of truth — recreate visually in SwiftUI / Jetpack Compose. Screenshots in `assets/design-system/project/assets/` (e.g. `network-screen-reference.png`) are pixel ground truth.

**Platform divergence is intentional**, not drift. The system explicitly specs different treatments per platform — most notably the contact-row avatar (iOS: lavender bg + indigo text, Android: solid Cobalt + white text). Don't unify without checking the spec first.

**Bundled fonts**: Bebas Neue is bundled in both apps for the wordmark. Use `Font.custom("BebasNeue-Regular", size:)` on iOS, `FontFamily(Font(R.font.bebas_neue))` on Android. The shared `WordmarkLockup` component on each platform is the canonical render.

**Updating the pinned design-system version**:
```
cd assets/design-system && git pull origin main && cd -
git add assets/design-system && git commit -m "Bump design-system pin"
```

## App Identity

- **App name**: Ticklr
- **Tagline**: Your People Matter
- **Bundle ID (iOS)**: `com.xaymaca.sit` · **Debug**: `com.xaymaca.sit.debug` (parallel install with App Store build)
- **Application ID (Android)**: `com.xaymaca.sit`
- **GitHub repo**: `github.com/vinny2020/ticklr`
- **Landing page**: `xaymaca.com/sit`
- **Privacy policy**: `xaymaca.com/sit/privacy`
- **Support URL**: `xaymaca.com/sit/support`
- **iOS App Store**: `apps.apple.com/us/app/ticklr/id6760884034`
- **Google Play**: `play.google.com/store/apps/details?id=com.xaymaca.sit`

## Feature Parity Status

| Feature | iOS | Android |
|---|---|---|
| Contact import (phone + LinkedIn CSV) | ✅ | ✅ |
| Groups / circles | ✅ | ✅ |
| Tickle calendar (recurring reminders) | ✅ | ✅ |
| Compose SMS/MMS with templates | ✅ | ✅ |
| Settings | ✅ | ✅ |
| Localization | ✅ Spanish | ✅ 12 languages |
| Unit tests | — | ✅ |
| Adaptive/Pulse icon | ✅ | ✅ |
| Debug seed / clear contacts | ✅ | ✅ |
| Live in store | ✅ App Store | ✅ Google Play |

## Debug Tools (gated by DEBUG flag — not in release builds)

Both platforms have a **Debug section** in Settings:
- **Load Test Contacts** — seeds 20 fake contacts from `assets/seed/test_contacts.csv`
- **Clear All Contacts** — wipes all contacts with confirmation

iOS: gated by `#if DEBUG` in `SettingsView.swift` via `SeedDataService.swift`
Android: gated by `BuildConfig.DEBUG` in `SettingsViewModel.kt` via `SeedDataService.kt`

## Platform-Specific SMS Behavior

**Both platforms now use the same handoff model:** the user's default SMS app opens with recipient and message body pre-filled, and the user sends from there. Neither app sends SMS silently. iOS never could (Apple policy); Android originally had a `SmsManager` direct-send path gated behind a Settings toggle, removed in v1.5.5-production for Google Play SMS/Call Log policy compliance.
