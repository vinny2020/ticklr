# Changelog

All notable changes to Ticklr: Your People Matter are documented here.

## [Unreleased] — 2026-04-12

### Android — Internationalization (Phase 1–3)

- **Phase 1 — String extraction**: All ~150 user-visible strings extracted from hardcoded Kotlin to `res/values/strings.xml`; every Compose screen now uses `stringResource(R.string.key)`; ViewModel toast strings use `context.getString()` via `@ApplicationContext`; `TickleFrequency` enum display names mapped to string resource IDs
- **Phase 2 — Locale-aware formatting**: Notification strings in `TickleWorker` and `TickleAlarmReceiver` replaced with `context.getString()` calls; confirmed no `SimpleDateFormat` with hardcoded locales exists in the codebase
- **Phase 3 — 12-language translation pack**: Added full translations for Spanish (`es`), French (`fr`), German (`de`), Italian (`it`), Dutch (`nl`), Greek (`el`), Polish (`pl`), Romanian (`ro`), Hungarian (`hu`), Portuguese (`pt`), Swedish (`sv`), Czech (`cs`); complex plural rules (one/few/many/other) correctly handled for Polish and Czech; `resourceConfigurations` in `build.gradle.kts` restricts bundled locales to the 13 supported languages; `TranslationCompletenessTest` fails CI if any key is missing from any translation file
- **Bug fix — bottom nav missing on Compose screen**: `currentDestination?.route` returns the full route pattern including query params (e.g. `"compose?contactId={contactId}"`); fixed by stripping query params and path segments before matching against `bottomNavRoutes`
- **Bug fix — duplicate `TickleAlarmReceiver` in `AndroidManifest.xml`**: Merged two `<receiver>` blocks into one with both `TICKLE_ALARM` and `BOOT_COMPLETED` intent actions
- **Bug fix — missing drawable `ic_notification`**: `TickleAlarmReceiver` referenced non-existent `R.drawable.ic_notification`; changed to `R.mipmap.ic_launcher`
- **Bug fix — nullable `versionName` in `SettingsScreen`**: `packageInfo.versionName` is nullable on API 33+; added `?: "1.0"` fallback
- **Tests**: `StringResourceTest` (10 test methods verifying all string resource keys exist at compile time); `TranslationCompletenessTest` (data-driven; covers all 12 translation files for both missing and extra keys)

## [Unreleased] — 2026-03-17

### iOS
- **Message Templates CRUD** — `TemplateListView` and `TemplateEditView` added; templates accessible from Settings → Message Templates; default "Checking in" template seeded on first launch
- **Settings expansion** — live contact count, tickle notification toggle (with system-denied deep link to iOS Settings), default tickle frequency picker, app version read from bundle at runtime
- **App icon** — full `AppIcon.appiconset` generated from Pulse SVG logo across all required sizes (20 → 1024px); Navy `#0A1628` background, App Store compliant (no alpha channel)
- **Search on ComposeView** — filter contacts by name, company, and job title while composing
- **LinkedIn import UX** — improved step-by-step guidance in ImportView

### Android
- **Full native app** — Kotlin + Jetpack Compose + Room + Hilt, feature parity with iOS: contact import (phone + LinkedIn CSV), groups, tickle reminders (WorkManager), SMS compose with direct send via SmsManager, onboarding, settings
- **Build warning fixes** — deprecated `Icons.Default.ArrowBack/Send/Message` replaced with `Icons.AutoMirrored` equivalents; `@OptIn(FlowPreview::class)` added; `@Index("groupId")` added to `ContactGroupCrossRef`; Room schema bumped to v2

## [0.1.0] — 2026-03-15

### Added
- Monorepo init with iOS and Android platform directories
- **iOS** — Swift 6, SwiftUI, SwiftData; full feature set: contacts, groups, tickle reminders, SMS compose, onboarding, settings
- **Android** — initial scaffold committed and flattened into monorepo
