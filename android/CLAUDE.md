# CLAUDE.md — Ticklr Android

> **Status:** Live on Google Play (April 29, 2026 as `android/v1.5.5-production`). The historical record of completed work — i18n phases (12 languages), ComposeScreen redesign, store submission cycle, closed-testing bug fixes, SEND_SMS removal — lives in `~/Documents/SecondBrain/Projects/Ticklr/CLAUDE Archive — Shipped Work.md`. This file tracks reference material and ongoing work only.

---

## 🛠️ Pending Tasks — Start Here

### Warm redesign — in flight on `feat/warm-redesign-android`

Major UI redesign for the next Play Store release. Branch is
feature-complete and pushed; not yet merged. See
`~/.claude/projects/-Users-xaymaca-Projects-ticklr/memory/project_warm_redesign.md`
for the full design decisions log shared with iOS.

**What's done on the branch:**
- Warm theme tokens (3 tiers × light + derived dark) in
  `ui/theme/WarmTheme.kt`, plus LocalWarmth + LocalWarmPalette
  composition locals.
- 5 canonical relationship categories (Family / Close Friends /
  Work / Milestones / Neighbors & Community) with stable string
  ids in `ui/theme/WarmCategory.kt`. Seeded as `ContactGroup`s on
  first launch by `data/repository/CanonicalGroupSeed.kt` via
  Room migration v3→v4 that added the `categoryId: String?` column.
- Material Icons Extended dep added (for Briefcase / CalendarMonth
  / Groups icons used by category badges).
- Bundled Noto SemiBold for ar/hi/ja headings + the existing
  Bebas Neue for Latin Subtle in `ui/theme/WarmType.kt`. OFL-1.1
  licenses under `android/licenses/`.
- Warm primitives in `ui/warm/`: WarmCard (Hero/Compact/Row),
  CategoryBadge, TicklePrompt (decorative), MonogramAvatar +
  MonogramPhotoAffordance, WarmFilterChip, WarmListContainer +
  WarmRowDivider, WarmEyebrow, WarmIllustration (Canvas-drawn).
- ContactPhotoView (3-state resolver), LocalPhotoStore (local-only
  JPEG cropped to 512px under filesDir/photos/), ContactPhotoService
  (read-only ContactsContract match by phone via PhoneLookup or
  email via Email.CONTENT_LOOKUP_URI; guards on READ_CONTACTS so
  it silently returns null on Samsung devices where the user
  hasn't granted access).
- ContactsAccessBanner on Contact Detail (TIC-32) — re-prompts or
  deep-links to Settings depending on the current permission state;
  re-checks on `ON_RESUME` so returning from Settings invalidates
  the photo cache automatically.
- 55 new `warm_*` string resources in all 21 shipped locales —
  `TranslationCompletenessTest` stays green.
- All 5 main tabs warmed (Network / Tickle / Groups / Compose /
  Settings) + Onboarding + Contact Detail. Unified 32sp warm
  heading across tabs.
- WarmCategoryTest covers stable-id contract + palette + string
  bindings.

**Post-device-test fixes shipped on the branch:**
- Network filter chip flicker eliminated — chip-count StateFlows
  are pre-built in NetworkViewModel instead of being recreated on
  every recomposition.
- Contact Detail action chips: drop Email (handled by user's mail
  app anyway), and let "Create a tickle" wrap to 2 lines so it
  stops truncating to "Create a".
- Compose screen wraps in `verticalScroll(rememberScrollState())`
  so the message field keeps its 120dp min height with the IME
  visible (was collapsing to a single-line strip; template
  dropdown + Send button were getting pushed off-screen too).
- `MessageTemplateSeed` is now DB-aware: re-inserts the default
  "Checking in" template when the templates table is empty
  regardless of the `hasSeededDefaultTemplates` prefs flag. Closes
  the previously-noted "default template never seeds" pending
  task — that bug bit users whose flag was set but row was wiped
  (Clear All Data, prior bug, etc.).

**What's left:**
- Open the PR, merge, ship.
- Translation review of LLM-generated locales (cs/de/el/es/fr/he/
  hu/it/ko/nl/pl/pt/ro/ru/sv/ur/zh-Hans). Tracked separately in
  Linear TIC-33.
- Optional: warm sub-screens (TemplateListScreen, ImportScreen,
  TickleEditScreen, AddContactScreen) still on system chrome.

---

### Task — Edge-to-edge cleanup for v1.5.6

**Why:** Play Console flagged two "recommended" warnings on the v1.5.5-production submission:

1. **"Edge-to-edge may not display for all users"** — Google's automated checker rendered screens on Android 15+ (API 35) and detected content not filling the full window properly.
2. **"Your app uses deprecated APIs or parameters for edge-to-edge"** — something in the code (or a dependency) is calling deprecated APIs like `Window.setStatusBarColor()`, `Window.setNavigationBarColor()`, or `View.setSystemUiVisibility()`. These are deprecated in Android 15 and don't compose well with the enforced edge-to-edge model.

These are non-blocking now — recommendations don't trigger rejection — but Google has a pattern of promoting "recommended" → "required" within a release cycle or two. Worth fixing as the v1.5.6 headline change before it becomes urgent.

**Investigation steps (in order):**

1. **Confirm `enableEdgeToEdge()` is being called.** Open `MainActivity.kt` and check `onCreate()`. The expected pattern is:
   ```kotlin
   import androidx.activity.enableEdgeToEdge
   override fun onCreate(savedInstanceState: Bundle?) {
       installSplashScreen()  // splash first
       enableEdgeToEdge()     // before super.onCreate
       super.onCreate(savedInstanceState)
       setContent { SITTheme { /* ... */ } }
   }
   ```
   If `enableEdgeToEdge()` is missing, that's likely the entire fix for warning #1.

2. **Search for deprecated bar-color / system-UI calls.** From the repo root:
   ```bash
   grep -rn "setStatusBarColor\|setNavigationBarColor\|setSystemUiVisibility\|SYSTEM_UI_FLAG_" android/app/src/main --include="*.kt"
   ```
   Any hits need to migrate. Replacements:
   - `setStatusBarColor` / `setNavigationBarColor` → set transparent in theme XML, let edge-to-edge + theme handle color via `windowLightStatusBar`/`windowLightNavigationBar`
   - `setSystemUiVisibility(...)` → `WindowCompat.getInsetsController(window, decorView).isAppearanceLightStatusBars = true/false`

3. **Search for `windowSoftInputMode`** in `AndroidManifest.xml`. With edge-to-edge, the system no longer auto-resizes for the IME (which is exactly the bug we hit during closed testing — see archive). The four screens with `Modifier.imePadding()` already handle this correctly: `AddContactScreen`, `TickleEditScreen`, `ComposeScreen`, `TemplateEditScreen`. Audit any new scrollable screens added since for the same fix.

4. **Audit theme XML** at `app/src/main/res/values/themes.xml` and `values-night/themes.xml`. Look for hardcoded `windowBackground`, `statusBarColor`, `navigationBarColor` that may conflict with edge-to-edge. Modern theme should set these to `@android:color/transparent` (or omit) and let the Compose `Surface` paint behind the system bars.

5. **Visual QA on Android 15.** Run the v1.5.5 build on an Android 15 emulator. Look for:
   - Status bar area showing a solid color block instead of letting content draw behind it
   - Bottom navigation bar with awkward gap or overlap
   - Any screen where content gets clipped by the system bars or visibly avoids them

6. **Re-verify against the warning messages** in Play Console after uploading the v1.5.6 candidate. They should disappear; if they don't, the diagnostic is incomplete.

**Scope:** `MainActivity.kt`, `themes.xml` / `values-night/themes.xml`, possibly individual screen files if any have hardcoded bar handling. No data model or business logic changes.

---

## 🐛 Smaller tech debt

### Higher-quality Android app icon

Current PNG mipmaps were generated via PIL and flagged as lower quality than the iOS icon. Regenerate using `cairosvg` from the SVG source for crisper rendering at all densities. Store-listing icon update is a separate manual step in Play Console.

### Dead code: `ui/launch/LaunchScreen.kt`

The Compose launch screen view is no longer referenced anywhere — replaced by the system splash screen via the AndroidX SplashScreen API. Delete or repurpose.

---

## 🔮 Future Considerations

> Not on the runway. Reference notes for if these ever come up.

### RTL (right-to-left) language support

Only needed if Arabic, Hebrew, Urdu, Farsi, or other RTL languages are added to `strings.xml`.
Currently `android:supportsRtl="false"` in `AndroidManifest.xml` since no RTL translations
ship — and forced-RTL on devices was mirroring our LTR layouts. To enable:

- Flip `android:supportsRtl="true"` in `AndroidManifest.xml`
- Audit Compose icons — switch directional ones (chevrons, send) to `Icons.AutoMirrored.*` variants. `SettingsScreen.kt` uses `Icons.Default.ChevronRight` in `SettingsRow` — that needs to become `Icons.AutoMirrored.Filled.ChevronRight`.
- Search the codebase for RTL-breaking patterns: `padding(left =`, `padding(right =`, `offset(x =`, `absoluteOffset(`. Replace with `padding(start = / end =)` where appropriate; hardcoded `x` offsets don't auto-flip.
- Verify `TicklrToast` centers correctly in RTL.
- Add `values-ar/strings.xml` (Arabic has 6 plural categories: zero/one/two/few/many/other — Android supports all).
- Add `"ar"` to `resourceConfigurations` in `build.gradle.kts`.
- Test on emulator with Arabic locale forced; budget time for visual QA on every screen.

This is the most labor-intensive phase if undertaken. Treat as a project unto itself.

---

## Architecture — MVVM + Repository

- **UI**: Jetpack Compose (no XML layouts)
- **State**: ViewModel + StateFlow
- **Persistence**: Room (version 2) + TypeConverters for `List<String>`
- **DI**: Hilt
- **Navigation**: Navigation Compose (`NavGraph.kt`)
- **Background work**: WorkManager (`TickleWorker`) + `AlarmManager` via `TickleAlarmReceiver`
- **Localization**: `res/values/strings.xml` + 20 translated locales (ar, cs, de, el, es, fr, he, hi, hu, it, ja, ko, nl, pl, pt, ro, ru, sv, ur, zh); `resourceConfigurations` restricts bundled to these 21.
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 35
- **SMS**: intent-only handoff via `Intent.ACTION_SENDTO` (`smsto:` URI). No `SEND_SMS` permission. (Removed in v1.5.5-production for Google Play SMS/Call Log policy compliance — see archive.)

## Project Structure

```
app/src/main/java/com/xaymaca/sit/
├── SITApp.kt                       # @HiltAndroidApp, PREFS_NAME, KEY_ONBOARDING_COMPLETE, KEY_THEME_MODE
├── MainActivity.kt                 # Entry point, NavGraph host, installSplashScreen()
├── data/
│   ├── model/                      # Contact, ContactGroup, ContactGroupCrossRef,
│   │                               # MessageTemplate, TickleReminder, Enums, Relations
│   ├── dao/                        # ContactDao, ContactGroupDao,
│   │                               # MessageTemplateDao, TickleReminderDao
│   ├── db/SITDatabase.kt           # Room DB v2, StringListConverter
│   └── repository/                 # ContactRepository, MessageTemplateRepository, TickleRepository
├── di/DatabaseModule.kt            # Hilt module — provides DB and DAOs
├── service/
│   ├── ContactImportService.kt     # ContactsContract import
│   ├── LinkedInCSVParser.kt        # CSV parsing (mirrors iOS implementation)
│   ├── SmsService.kt               # Intent.ACTION_SENDTO handoff (no direct send)
│   ├── StringListConverter.kt      # Room TypeConverter for List<String> — Gson-free, R8-safe
│   ├── TickleScheduler.kt          # nextDueDate logic + WorkManager scheduling
│   ├── TickleWorker.kt             # WorkManager Worker for notifications
│   └── TickleAlarmReceiver.kt      # BroadcastReceiver — TICKLE_ALARM + ACTION_BOOT_COMPLETED
├── ui/
│   ├── theme/                      # Color, Theme, Type (Navy/Cobalt/Amber, dark-first M3)
│   ├── nav/                        # NavGraph, Screen
│   ├── launch/LaunchScreen.kt      # ⚠️ Dead code — replaced by system splash, deletion candidate
│   ├── onboarding/                 # OnboardingScreen, ImportScreen
│   ├── network/                    # NetworkListScreen, NetworkViewModel,
│   │                               # ContactDetailScreen, AddContactScreen
│   ├── groups/                     # GroupListScreen, GroupDetailScreen, GroupViewModel
│   ├── tickle/                     # TickleListScreen, TickleEditScreen, TickleViewModel
│   ├── compose/                    # ComposeScreen, ComposeViewModel
│   ├── settings/                   # SettingsScreen, SettingsViewModel,
│   │                               # TemplateListScreen, TemplateEditScreen, TemplateViewModel
│   └── shared/                     # TagChipRow, TicklrToast (Cobalt overlay)
└── tests/
    ├── LinkedInCSVParserTest.kt
    ├── StringListConverterTest.kt
    ├── TickleSchedulerTest.kt
    ├── ScreenRouteTest.kt
    ├── ContactRepositoryTest.kt
    ├── StringResourceTest.kt          # Compile-time string resource coverage
    └── TranslationCompletenessTest.kt # Data-driven — adding a language is a 1-line map entry
```

## Key Notes

- `SITApp.PREFS_NAME` + `SITApp.KEY_ONBOARDING_COMPLETE` — SharedPreferences keys for onboarding state
- Room DB is version 2 — any schema changes need a migration
- `ContactGroupCrossRef` handles the many-to-many Contact ↔ Group relationship
- `StringListConverter`, `ContactFingerprint`, and `parseJsonStringArray()` helper are Gson-free / R8-safe
- WorkManager handles tickle notifications; `TickleAlarmReceiver` handles `ACTION_BOOT_COMPLETED` so alarms persist across reboots
- `AddContactScreen` doubles as edit screen via optional `contactId` parameter
- `TicklrToast` is the shared `Box`-overlay toast composable in `ui/shared/` — used by GroupDetailScreen, ComposeScreen, TickleEditScreen
- All user-visible strings go through `stringResource(R.string.x)`; ViewModel toasts use `context.getString()` via `@ApplicationContext`
- Brand strings ("Ticklr", "Xaymaca", "YOUR PEOPLE MATTER") stay untranslated across all locales

## Pulse Brand in Compose

```kotlin
val Navy   = Color(0xFF0A1628)   // backgrounds
val Cobalt = Color(0xFF2563EB)   // primary actions
val Amber  = Color(0xFFF5C842)   // accent, tickle due state
```

## Permissions (AndroidManifest)

- `READ_CONTACTS` — contacts import
- `POST_NOTIFICATIONS` — tickle reminders (Android 13+)
- `RECEIVE_BOOT_COMPLETED` — re-register alarms after device reboot
- `SCHEDULE_EXACT_ALARM` — exact-time tickle alarms
- `RECEIVE_SMS` is defensively *removed* via `tools:node="remove"` in case any dependency tries to merge it in (same pattern as `AD_ID`)
- ⚠️ **Do NOT add `SEND_SMS` back.** Google Play categorically prohibits it for non-default-SMS-handler apps. SMS goes through `Intent.ACTION_SENDTO` only.

## Build & Run

Open `android/` in Android Studio (Hedgehog or newer).
Run on API 26+ emulator or physical Android device.

CI builds signed AABs via GitHub Actions on tags matching `android/v*-{alpha|beta|production}`.
See `~/Documents/SecondBrain/Projects/Ticklr/Release Tagging Guide.md` for tag conventions.
`versionCode` is auto-computed as `100 + GITHUB_RUN_NUMBER`.

## Sensitive Files — Never Commit

`keystore.jks`, `*.keystore`, `release.properties`, `google-services.json`, any file
containing signing passwords or API keys. Production keystore lives at
`~/Documents/ticklr-release.keystore` (alias: `ticklr`); credentials in `local.properties`.
