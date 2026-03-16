# CLAUDE.md — SIT: Stay in Touch (Monorepo)

## What This Repo Is

SIT is a privacy-first personal network manager with two native platform implementations.
All data is stored on-device. No cloud, no analytics, no account required.

## Repo Structure

```
sit/
├── ios/        — Swift 6, SwiftUI, SwiftData (see ios/CLAUDE.md for full iOS spec)
├── android/    — Kotlin, Jetpack Compose, Room (see android/CLAUDE.md for full Android spec)
└── assets/brand/ — Shared Pulse logo SVGs and color tokens
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
| Wordmark | Syne 800 | "SIT" logotype |
| Tagline font | Syne 400 | "STAY IN TOUCH" |

## Feature Parity Target

Both platforms should implement:
1. Contact import (phone contacts + LinkedIn CSV)
2. Groups / circles
3. Tickle calendar (recurring reminders)
4. Compose SMS/MMS with templates
5. Settings

## Platform-Specific Notes

**iOS** — `MFMessageComposeViewController` for messaging (requires user tap to send)
**Android** — `Intent.ACTION_SENDTO` for SMS, or `SmsManager` for direct send (requires SEND_SMS permission)

> Android CAN send SMS silently with `SmsManager` if the user grants SEND_SMS permission.
> This is a meaningful UX advantage over iOS worth surfacing in the Android experience.
