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
| Wordmark | Syne 800 | "Ticklr" logotype |
| Tagline | Syne 400 | "YOUR PEOPLE MATTER" |

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
