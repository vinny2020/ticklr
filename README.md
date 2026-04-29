# Ticklr — Your People Matter

A privacy-first personal network manager for iOS and Android. Build your contact network, stay connected on your schedule with the tickle calendar, and send messages — all stored locally with no cloud, no account required.

## Screenshots

_iOS shown — Android UI mirrors with platform-native treatments._

| Onboarding | Network | Import |
|:----------:|:-------:|:------:|
| <img src="docs/screenshots/05-compose.png" width="220"/> | <img src="docs/screenshots/01-network-list.png" width="220"/> | <img src="docs/screenshots/02-contact-detail.png" width="220"/> |

| New Tickle | New Group | Groups |
|:----------:|:---------:|:------:|
| <img src="docs/screenshots/04-tickle-edit.png" width="220"/> | <img src="docs/screenshots/06-launch-screen.png" width="220"/> | <img src="docs/screenshots/07-settings.png" width="220"/> |

## Repository Structure

```
sit/
├── ios/          — Native Swift 6 / SwiftUI / SwiftData (iOS 17+)
├── android/      — Native Kotlin / Jetpack Compose / Room (Android 8+)
├── assets/
│   └── brand/    — Shared logo SVGs, color palette, design tokens
├── CLAUDE.md     — Top-level Claude Code context
└── README.md
```

## Philosophy
- Everything on-device — zero cloud sync required
- Import contacts from phone or LinkedIn CSV export
- Tickle calendar: recurring reminders to reach out on your schedule
- Send via native SMS/MMS — no third-party messaging

## Platforms

| Platform | Language | UI | Persistence | Min Version |
|---|---|---|---|---|
| iOS | Swift 6 | SwiftUI | SwiftData | iOS 17 |
| Android | Kotlin | Jetpack Compose | Room | Android 8 (API 26) |

## Brand

**Pulse identity** — Navy `#0A1628` · Cobalt `#2563EB` · Amber `#F5C842`
Wordmark: Bebas Neue 400 — "Ticklr"
Tagline: Inter 400, 10px, 0.16em tracking, amber @ 50% — "YOUR PEOPLE MATTER"
Body: Inter 400/500/600/700
See `assets/design-system/` for the full token reference and brand assets.

## Getting Started

### iOS
```bash
cd ios
brew install xcodegen
xcodegen generate
open SIT.xcodeproj
```

### Android
```bash
cd android
./gradlew assembleDebug
```
Or open `android/` in Android Studio.

## Roadmap
- [x] iOS scaffold + SwiftData models
- [x] Pulse brand identity + launch screen
- [x] iOS tickle calendar feature
- [x] Android scaffold + full feature parity
- [x] iOS contacts import (CNContactStore + LinkedIn CSV)
- [x] Android contacts import (ContactsContract + LinkedIn CSV)
- [x] App Store submission
- [ ] Google Play submission

Built by [Xaymaca](https://xaymaca.com) — Build Smarter with AI.
