# CLAUDE.md — Ticklr Android

> **Status:** Live on Google Play (April 29, 2026 as `android/v1.5.5-production`). The historical record of completed work — i18n phases (12 languages), ComposeScreen redesign, store submission cycle, closed-testing bug fixes, SEND_SMS removal — lives in `~/Documents/SecondBrain/Projects/Ticklr/CLAUDE Archive — Shipped Work.md`. This file tracks reference material and ongoing work only.

---

## 🛠️ Pending Tasks — Start Here

### Task — Move default-template seeding to app launch

**Bug:** The default "Checking in" `MessageTemplate` is only seeded when the user opens
**Settings → Message Templates** (`TemplateListScreen.kt` `LaunchedEffect` calls
`TemplateViewModel.seedDefaultIfNeeded`). If a user never visits that screen, the template
dropdown in `ComposeScreen` — gated by `if (templates.isNotEmpty())` — stays hidden forever.

**Current band-aid:** `ComposeScreen` ALSO calls `templateViewModel.seedDefaultIfNeeded(prefs)`
in a `LaunchedEffect`. Idempotent (guarded by `hasSeededDefaultTemplates` SharedPrefs flag),
but every screen that depends on templates would need to repeat this pattern. Same latent
bug exists on iOS — see `ios/CLAUDE.md`.

**Real fix:** Seed once at app launch from `SITApp.onCreate()`.
1. Inject `MessageTemplateRepository` and `@ApplicationContext context: Context` into
   `SITApp` via Hilt (it's already `@HiltAndroidApp`).
2. In `onCreate()`, after `super.onCreate()`, kick off a one-time coroutine on
   `Dispatchers.IO` (or use a `WorkManager` `OneTimeWorkRequest`) that:
   - Reads the `hasSeededDefaultTemplates` flag from `getSharedPreferences(PREFS_NAME, MODE_PRIVATE)`
   - If unset, inserts the default template (`title = "Checking in"`,
     `body = "Hey! Just checking in — hope you're doing well. Let's catch up soon!"`)
   - Sets the flag
3. **Remove** the band-aid `LaunchedEffect` from `ComposeScreen.kt` that calls
   `templateViewModel.seedDefaultIfNeeded`, and the `TemplateViewModel` parameter on
   `ComposeScreen`.
4. **Keep** the `LaunchedEffect` in `TemplateListScreen.kt` for now — it's still idempotent
   and provides a safety net if the launch-time seed ever fails. Or remove it for purity;
   either is defensible.
5. Bonus: extract the seed string + body into `MessageTemplateSeed.kt` so the launch path
   and any defensive paths share one source of truth.

**Scope:** `SITApp.kt` (~10 lines), `ComposeScreen.kt` (revert band-aid, ~5 lines),
optionally `MessageTemplateSeed.kt` (new helper).

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

### Task — RTL (right-to-left) language support (optional / future)

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
- **Localization**: `res/values/strings.xml` + 12 translated locales (es, fr, de, it, nl, el, pl, ro, hu, pt, sv, cs); `resourceConfigurations` restricts bundled to these 13.
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
