# CLAUDE.md — Ticklr Android

---

## ✅ Completed Tasks

### ~~Closed-testing bug fixes — keyboard IME padding, Compose tab selection, system splash screen~~ ✅ DONE (2026-04-21)

**Context:** Second round of tester feedback from alpha closed-testing cycle.

**Issue 1 — Tags and Notes fields covered by keyboard on Add Contact screen:**
Root cause: `enableEdgeToEdge()` in MainActivity disables the system's automatic window-resize-for-IME behavior. The scrollable containers in the form screens weren't reading IME insets, so the soft keyboard floated over the bottom fields.

**Fix:** Added `Modifier.imePadding()` to the top-level scrollable container in:
- `ui/network/AddContactScreen.kt` — the one testers reported
- `ui/tickle/TickleEditScreen.kt` — same root-cause bug on the Note field
- `ui/compose/ComposeScreen.kt` — same bug on the Message body
- `ui/settings/TemplateEditScreen.kt` — same bug on the Body field

All four edits are one-liners — `androidx.compose.foundation.layout.*` is already wildcard-imported in each file, so no new import lines were needed.

**Issue 2 — Compose tab never appears highlighted in the bottom nav:**
Root cause: in `NavGraph.kt`, the Compose destination is registered with a query parameter suffix `"compose?contactId={contactId}"`, but `Screen.Compose.route` is just `"compose"`. The `selected` predicate on `NavigationBarItem` compared full routes with `it.route == item.screen.route`, which never matched for Compose.

**Fix:** Normalized the `selected` predicate with the same `substringBefore('?')` + `substringBefore('/')` logic already used by `showBottomBar`:
```kotlin
selected = currentDestination?.hierarchy?.any {
    it.route?.substringBefore('?')?.substringBefore('/') == item.screen.route
} == true,
```
Now Compose highlights correctly like the other four tabs, including when arriving from ContactDetail's "Message" deep-link that passes a `contactId` query param.

**Issue 3 — Blank flash on cold launch:**
Root cause: there was no system-level splash screen. The existing `ui/launch/LaunchScreen.kt` Compose view (animated Ticklr logo) was dead code — never referenced anywhere. Users saw a blank window between launcher-icon tap and first Compose frame.

**Fix:** Adopted the AndroidX SplashScreen API (backport, works on API 23+):
- `libs.versions.toml` — added `core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version = "1.0.1" }`
- `app/build.gradle.kts` — `implementation(libs.core.splashscreen)` added next to Gson
- `res/values/colors.xml` — new file with `<color name="ticklr_navy">#1C3A62</color>` (matches LaunchScreen BG_COLOR and the launcher icon background exactly)
- `res/values/themes.xml` — new `Theme.SIT.Splash` parented to `Theme.SplashScreen`, with:
  - `windowSplashScreenBackground` = `@color/ticklr_navy`
  - `windowSplashScreenAnimatedIcon` = `@mipmap/ic_launcher_foreground`
  - `postSplashScreenTheme` = `@style/Theme.SIT` (post-splash handoff)
- `AndroidManifest.xml` — `<application android:theme="@style/Theme.SIT.Splash">`
- `MainActivity.onCreate` — `installSplashScreen()` called **before** `super.onCreate(...)`

The dead `ui/launch/LaunchScreen.kt` Compose view is still in the tree; it can be deleted later if wanted, or repurposed as a secondary post-splash animation.

**No string resources added** — no translation drift. All 35 Gradle unit-test tasks pass (`TranslationCompletenessTest` + `StringResourceTest` both green).

---

### ~~Closed-testing bug fixes — RTL layout mirroring + Save button invisible disabled state~~ ✅ DONE (2026-04-17)

**Context:** Tester feedback from alpha closed-testing cycle reported three issues.

**Issue 1 — RTL layout on Compose / Settings / Network Add screens:**
Root cause: `android:supportsRtl="true"` in `AndroidManifest.xml`. On any device with system layout direction forced to RTL (Arabic locale, or the "Force RTL layout direction" developer option on Samsung/Pixel), Compose mirrors all layouts. The project ships zero RTL translations (no `values-ar/-iw/-ur/-fa` directories), so RTL serves no purpose.

**Fix:** `android:supportsRtl="false"` in `AndroidManifest.xml` — one-line change.
Note: If an RTL language is added in future, flip this back to `true` and run the full icon/padding audit described in the Task 0d section below.

**Issue 2 — Network tab sitting on right edge of bottom nav, uneven spacing:**
Same root cause as Issue 1. Network is the *first* (leftmost) item in `bottomNavItems` — RTL mirroring moved it to the right. Resolved automatically by the manifest fix.

**Issue 3 — Save button on Add-Tickle screen does nothing, no feedback:**
Root cause: The button's enabled condition (`isLoaded && selectedContact != null || selectedGroup != null`) worked correctly, but the hardcoded `color = Amber` on the `Text` composable overrode Material 3's automatic disabled-state alpha. So the button appeared identically bright whether enabled or disabled — testers typed in the search field, didn't tap a contact row to select it, hit a visually-active "Save", and got silent non-response.

**Fix:** Lifted the condition to `val canSave = isLoaded && (selectedContact != null || selectedGroup != null)`, passed it to `enabled = canSave`, and changed the Text color to `if (canSave) Amber else Amber.copy(alpha = 0.38f)`. Disabled state is now visibly faded.

**Files changed:**
- `app/src/main/AndroidManifest.xml` — `supportsRtl` false
- `app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt` — `canSave` val + conditional Text alpha

**No string resources added.** No translation files touched. All existing tests continue to pass.

---

### ~~Task 0 — Phase 1 Internationalization: Extract all hardcoded strings to `strings.xml`~~ ✅ DONE (2026-04-12)

**Summary of what was done:**
- Populated `res/values/strings.xml` with ~150 keys covering all Compose screens
- Replaced every hardcoded string in all 14 screen files with `stringResource(R.string.key)`
- ViewModel toast strings (`TickleViewModel`, `ComposeViewModel`, `GroupViewModel`) use `context.getString()` via `@ApplicationContext`
- `TickleFrequency` enum display names mapped to resource IDs via `FrequencyExtensions.kt`
- `TickleAlarmReceiver.kt` created (was missing from repo) with `context.getString()` for notification strings
- `StringResourceTest.kt` — 10 test methods verifying all string resource keys exist at compile time
- Bug fixes along the way: duplicate `<receiver>` in AndroidManifest, missing `ic_notification` drawable, nullable `versionName` in SettingsScreen, bottom nav missing on Compose screen (route query-param stripping)

---

### ~~Task 0b — Phase 2: Locale-aware date/time and number formatting~~ ✅ DONE (2026-04-12)

**Summary of what was done:**
- Audited entire codebase — no `SimpleDateFormat` with hardcoded locales found
- Notification strings in `TickleWorker` and `TickleAlarmReceiver` now use `context.getString(R.string.tickle_notification_title/body)`
- Added `StringResourceTest` methods for notification key coverage
- Date math (millisecond arithmetic in `TickleListScreen`) is locale-neutral — no changes needed

---

### ~~Task 0c — Phase 3: Add first non-English language (Spanish)~~ ✅ DONE (2026-04-12) — extended to 12 languages

**Summary of what was done:**
- Created full translations for 12 languages: Spanish (`es`), French (`fr`), German (`de`), Italian (`it`), Dutch (`nl`), Greek (`el`), Polish (`pl`), Romanian (`ro`), Hungarian (`hu`), Portuguese (`pt`), Swedish (`sv`), Czech (`cs`)
- Complex plural rules correctly handled: Polish and Czech use `one/few/many/other`; Romanian uses `one/few/other`
- `resourceConfigurations` in `build.gradle.kts` restricts bundled locales to the 13 supported (`en` + 12 above)
- `TranslationCompletenessTest.kt` — data-driven test catches missing or extra keys in all 12 translation files; adding a new language requires only one map entry

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
3. **Remove** the band-aid `LaunchedEffect` from `ComposeScreen.kt` (lines that call
   `templateViewModel.seedDefaultIfNeeded`) and the `TemplateViewModel` parameter on
   `ComposeScreen`.
4. **Keep** the `LaunchedEffect` in `TemplateListScreen.kt` for now — it's still idempotent
   and provides a safety net if the launch-time seed ever fails. Or remove it for purity;
   either is defensible.
5. Bonus: extract the seed string + body into `MessageTemplateSeed.kt` so the launch path
   and any defensive paths share one source of truth.

**Why launch-time:** the dropdown should work on first install regardless of which tab the
user opens first. The current per-screen pattern is fragile to copy-paste mistakes — every
new screen that consumes templates needs to remember to seed.

**Scope:** `SITApp.kt` (~10 lines), `ComposeScreen.kt` (revert band-aid, ~5 lines),
optionally `MessageTemplateSeed.kt` (new helper).

---

### Task 0 — Phase 1 Internationalization: Extract all hardcoded strings to `strings.xml`

**Goal:** Every user-visible string in the Android app must come from `strings.xml` so that
adding a new language later is a translation-only task — no code changes required.

**Approach — Use Android's standard `strings.xml` resource system**

Android's native string resource system (`res/values/strings.xml`) is the standard approach.
Jetpack Compose accesses these via `stringResource(R.string.key)`. This gives us compile-time
key checking, pluralization via `plurals`, and compatibility with all translation tools.

**Step-by-step:**

1. **Populate `app/src/main/res/values/strings.xml`**

   Currently this file only contains `<string name="app_name">Ticklr</string>`. Add every
   user-visible string from the Compose UI screens. Use snake_case keys with screen prefix.

   **Naming convention:** `screen_context_element`. Examples:
   ```xml
   <!-- Common -->
   <string name="common_save">Save</string>
   <string name="common_cancel">Cancel</string>
   <string name="common_delete">Delete</string>
   <string name="common_done">Done</string>
   <string name="common_edit">Edit</string>
   <string name="common_ok">OK</string>
   <string name="common_reset">Reset</string>
   <string name="common_create">Create</string>

   <!-- Settings -->
   <string name="settings_title">Settings</string>
   <string name="settings_section_appearance">Appearance</string>
   <string name="settings_section_data">Data</string>
   <string name="settings_section_messaging">Messaging</string>
   <string name="settings_section_about">About</string>
   <string name="settings_section_developer">Developer</string>
   <string name="settings_theme_title">Theme</string>
   <string name="settings_theme_system">System Default</string>
   <string name="settings_theme_light">Light</string>
   <string name="settings_theme_dark">Dark</string>
   <string name="settings_import_title">Import Contacts</string>
   <string name="settings_import_subtitle">From phone or LinkedIn CSV</string>
   <string name="settings_templates_title">Message Templates</string>
   <string name="settings_templates_subtitle">Create and manage reusable messages</string>
   <string name="settings_sms_direct_title">Send SMS directly</string>
   <string name="settings_sms_direct_subtitle">Uses SmsManager instead of opening Messages app.</string>
   <string name="settings_about_privacy">Privacy-first. No cloud, no analytics, no account required. All data is stored on-device.</string>
   <string name="settings_about_built_by">Built by Xaymaca</string>
   <string name="settings_about_version">Version %1$s</string>
   <string name="settings_reset_onboarding_title">Reset Onboarding</string>
   <string name="settings_reset_onboarding_subtitle">Show the welcome screen again</string>
   <string name="settings_reset_confirm_message">This will return you to the welcome screen. Your data will not be deleted.</string>
   <string name="settings_clear_data_title">Clear All Data</string>
   <string name="settings_clear_data_message">This will permanently delete all contacts, groups, and tickles. This cannot be undone.</string>
   <string name="settings_clear_data_confirm">Clear All</string>
   <string name="settings_choose_theme">Choose Theme</string>

   <!-- Tickle list -->
   <string name="tickle_list_title">Tickle</string>
   <string name="tickle_list_section_due">Due</string>
   <string name="tickle_list_section_upcoming">Upcoming</string>
   <string name="tickle_list_section_snoozed">Snoozed</string>
   <string name="tickle_list_empty_title">No tickles yet</string>
   <string name="tickle_list_empty_description">Add one from a contact\'s detail page, or tap +</string>
   <!-- ... etc for all screens -->
   ```

   For strings with arguments, use positional parameters:
   ```xml
   <string name="tickle_row_overdue">%1$d d overdue</string>
   <string name="tickle_row_upcoming">In %1$d d</string>
   <string name="group_detail_member_added">%1$s added to %2$s</string>
   <string name="group_detail_member_added_generic">%1$s added to group</string>
   ```

   For plurals:
   ```xml
   <plurals name="contacts_count">
       <item quantity="one">%1$d contact</item>
       <item quantity="other">%1$d contacts</item>
   </plurals>
   ```

2. **Replace hardcoded strings in every Compose screen file**

   **Pattern — before:**
   ```kotlin
   Text("Settings", fontWeight = FontWeight.Bold)
   ```

   **Pattern — after:**
   ```kotlin
   Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold)
   ```

   For strings with arguments:
   ```kotlin
   // Before:
   Text("Version $versionName")
   // After:
   Text(stringResource(R.string.settings_about_version, versionName))
   ```

   **Import needed in each file:**
   ```kotlin
   import androidx.compose.ui.res.stringResource
   ```

   **Files to modify (all in `app/src/main/java/com/xaymaca/sit/ui/`):**
   - `settings/SettingsScreen.kt` (~30 strings)
   - `settings/TemplateListScreen.kt`
   - `settings/TemplateEditScreen.kt`
   - `tickle/TickleListScreen.kt` (~10 strings)
   - `tickle/TickleEditScreen.kt` (~15 strings)
   - `network/NetworkListScreen.kt`
   - `network/ContactDetailScreen.kt` (~20 strings)
   - `network/AddContactScreen.kt`
   - `groups/GroupListScreen.kt`
   - `groups/GroupDetailScreen.kt`
   - `compose/ComposeScreen.kt`
   - `onboarding/OnboardingScreen.kt`
   - `onboarding/ImportScreen.kt` (~15 strings)
   - `launch/LaunchScreen.kt`
   - `nav/NavGraph.kt` (bottom nav tab labels if hardcoded there)
   - `shared/TicklrToast.kt` (if it has any hardcoded text)

3. **Localize `TickleFrequency` and other enum display names**

   The `TickleFrequency` enum in `data/model/Enums.kt` likely has display names used in UI.
   Add a `@Composable` extension or a `displayNameResId` property that maps to string resources:
   ```kotlin
   val TickleFrequency.displayNameResId: Int
       get() = when (this) {
           TickleFrequency.DAILY   -> R.string.frequency_daily
           TickleFrequency.WEEKLY  -> R.string.frequency_weekly
           TickleFrequency.MONTHLY -> R.string.frequency_monthly
           TickleFrequency.CUSTOM  -> R.string.frequency_custom
       }
   ```
   Then in Compose UI: `Text(stringResource(frequency.displayNameResId))`

   Do the same for any other enums with user-facing display names (`TickleStatus`, `ImportSource`
   if they appear in UI).

4. **Handle strings in ViewModels**

   Some strings may be constructed in ViewModels (e.g., toast messages like `"Tickle saved"`,
   `"Message sent ✓"`, seed result messages). These are tricky because ViewModels don't have
   access to `stringResource()`.

   **Preferred approach:** Pass string resource IDs (or sealed result classes) from the ViewModel
   to the UI layer, and resolve them to strings in the `@Composable`:
   ```kotlin
   // In ViewModel — emit a sealed class or resource ID:
   sealed class ToastMessage {
       data class Res(@StringRes val resId: Int, val args: List<Any> = emptyList()) : ToastMessage()
   }
   _toastMessage.value = ToastMessage.Res(R.string.tickle_saved)

   // In Composable — resolve:
   val toast by viewModel.toastMessage.collectAsState()
   toast?.let {
       when (it) {
           is ToastMessage.Res -> stringResource(it.resId, *it.args.toTypedArray())
       }
   }
   ```

   **Alternatively** (simpler but less pure): Use `context.getString(R.string.key)` in
   ViewModels where `Application` context is available via `@HiltViewModel` + `@ApplicationContext`.
   This is acceptable for toast messages but don't overuse it.

   **Check these ViewModels for hardcoded strings:**
   - `settings/SettingsViewModel.kt` (seed messages, clear messages)
   - `tickle/TickleViewModel.kt` ("Tickle saved", "Tickle updated")
   - `compose/ComposeViewModel.kt` ("Message sent ✓")
   - `groups/GroupViewModel.kt` (toast messages)
   - `network/NetworkViewModel.kt`

5. **Do NOT localize these:**
   - Debug-only strings gated by `BuildConfig.DEBUG` (seed messages, debug button labels) —
     actually, DO localize debug button labels since they still show in the UI during development.
     But seed result messages ("Loaded X test contacts") can stay hardcoded.
   - Room entity field names, database column names, SharedPreferences keys
   - Icon content descriptions that are `null` (decorative icons)
   - Brand name "Ticklr" when used as the app name (use `R.string.app_name` which already exists)
   - Navigation route strings in `Screen.kt`

6. **Add `stringResource` import to `contentDescription` attributes**

   Any `contentDescription` currently set to a hardcoded English string should also be extracted.
   Any `contentDescription = null` (decorative) can stay as-is.

7. **Add unit tests for string resource coverage**

   Create `app/src/test/java/com/xaymaca/sit/StringResourceTest.kt`:
   ```kotlin
   import org.junit.Test
   import kotlin.test.assertTrue

   class StringResourceTest {
       /**
        * Verify that all string resource fields exist in R.string.
        * This test catches typos in resource names at compile time via R.string references.
        * If any key is missing from strings.xml, this file won't compile.
        */
       @Test
       fun `critical string resources exist`() {
           // These references will fail to compile if the keys don't exist in strings.xml
           val keys = listOf(
               R.string.settings_title,
               R.string.common_save,
               R.string.common_cancel,
               R.string.tickle_list_title,
               R.string.tickle_list_empty_title,
               R.string.settings_about_version,
           )
           assertTrue(keys.all { it != 0 }, "All string resource IDs should be non-zero")
       }
   }
   ```

   Also add a **Compose UI test** if instrumented tests are set up, or at minimum verify the
   app compiles cleanly with no unresolved `R.string` references (the compiler catches this).

8. **Verify the app builds and all existing + new tests pass**

   ```bash
   cd android
   ./gradlew assembleDebug 2>&1 | tail -20
   ./gradlew testDebugUnitTest 2>&1 | tail -20
   ```

**Gotchas & constraints:**

- **Do not break the running app.** Every `stringResource(R.string.xyz)` call must have a
  matching `<string name="xyz">` entry in `strings.xml`, or the build will fail at compile time.
  This is actually safer than iOS — if you miss a key, it won't compile. Work file-by-file anyway.
- **`stringResource()` is a `@Composable` function.** It can only be called inside `@Composable`
  scope. For ViewModel strings, use the sealed class pattern or `context.getString()` as described
  in step 4.
- **Escape apostrophes in XML.** Use `\'` or wrap in `"..."`:
  ```xml
  <string name="tickle_list_empty_description">Add one from a contact\'s detail page, or tap +</string>
  ```
- **Do NOT use `@android:string/` system strings** for app-specific text. Only use them for
  truly generic platform strings if needed.
- **`SettingsSectionHeader` and `SettingsRow` composables** in `SettingsScreen.kt` accept
  `String` parameters. The callers should pass `stringResource(...)` — no changes needed to
  the composable signatures themselves.
- **Android lint may warn about hardcoded strings** after partial migration. Suppress with
  `@Suppress("HardcodedText")` on any intentionally un-localized strings, or finish the full
  migration in one pass.
- **Proguard/R8:** No special rules needed — string resources are not affected by minification.
- **Do NOT add any new language resource directories yet** (no `values-es/`, `values-fr/`, etc.).
  This task is English-only string extraction. Adding languages is Phase 2.
- **The `build.gradle.kts` does not currently specify `resourceConfigurations`.** You may want
  to add this to `defaultConfig` to limit bundled languages once Phase 2 adds translations:
  ```kotlin
  resourceConfigurations += listOf("en")  // expand later: "es", "fr", etc.
  ```
  But this is optional for Phase 1.

**Scope:** All Compose screen files + `strings.xml` + enum display name extensions + ViewModel
toast refactoring + new test file. No Room schema changes, no navigation changes, no new
dependencies.

---

### ~~Task 0b — Phase 2: Locale-aware date/time and number formatting~~ ✅ DONE (2026-04-12)

**Prerequisite:** Task 0 (Phase 1) must be complete — all UI strings extracted to `strings.xml`.

**Goal:** Ensure all dates, times, and numbers displayed in the app respect the user's locale
settings, so that when translations are added in Phase 3, dates render correctly (e.g.
"11 avr. 2026" in French vs "Apr 11, 2026" in English).

**What needs to change:**

1. **Audit date formatting in `TickleListScreen.kt`**

   The `relativeDateLabel()` function (around line 308) currently uses millisecond arithmetic
   to compute day differences and `stringResource()` for the labels ("Today", "Tomorrow", etc.).
   This is fine for the relative labels (already localized in Phase 1).

   **However**, if there's a fallback that shows an actual formatted date (e.g. for dates more
   than 7 days out), ensure it uses `DateTimeFormatter` with the device locale:
   ```kotlin
   // Good — locale-aware:
   val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
       .withLocale(Locale.getDefault())
   val formatted = Instant.ofEpochMilli(timestamp)
       .atZone(ZoneId.systemDefault())
       .format(formatter)

   // Bad — hardcoded format:
   SimpleDateFormat("MMM dd, yyyy", Locale.US)  // ← breaks in non-English
   ```

   **Search for `SimpleDateFormat` across the codebase.** Any instance with a hardcoded
   `Locale.US` or `Locale.ENGLISH` must be changed to `Locale.getDefault()`, or better yet,
   replaced with `java.time.format.DateTimeFormatter.ofLocalizedDate()`.

   **Files to check:**
   - `ui/tickle/TickleListScreen.kt`
   - `ui/tickle/TickleEditScreen.kt`
   - `service/TickleScheduler.kt`
   - Any other file importing `SimpleDateFormat` or `DateTimeFormatter`

2. **Replace manual relative date logic (optional improvement)**

   Android provides `DateUtils.getRelativeTimeSpanString()` which auto-localizes relative
   dates ("2 days ago", "in 3 days") and handles pluralization across locales:
   ```kotlin
   import android.text.format.DateUtils
   val relativeLabel = DateUtils.getRelativeTimeSpanString(
       timestamp,
       System.currentTimeMillis(),
       DateUtils.DAY_IN_MILLIS,
       DateUtils.FORMAT_ABBREV_RELATIVE
   ).toString()
   ```

   **Decision point (same as iOS):**

   **Option A (Recommended — keep manual, it's already localized):**
   The Phase 1 `stringResource()` calls with `plurals` resources already handle this.
   The current approach gives more UI control ("3d overdue" vs "3 days ago"). Keep it.
   Just make sure the `plurals` XML entries are correct:
   ```xml
   <plurals name="tickle_row_days_overdue">
       <item quantity="one">%1$d d overdue</item>
       <item quantity="other">%1$d d overdue</item>
   </plurals>
   <plurals name="tickle_row_in_days">
       <item quantity="one">In %1$d d</item>
       <item quantity="other">In %1$d d</item>
   </plurals>
   ```

   **Option B (Use DateUtils):** Replace `relativeDateLabel()` entirely. Loses "overdue"
   framing. Not recommended.

3. **Verify number formatting**

   If any screen displays contact counts or other numbers, ensure they use
   `NumberFormat.getInstance()` or Compose's built-in formatting:
   ```kotlin
   // Good:
   Text(NumberFormat.getInstance().format(contactCount))
   // Also good (if just displaying an Int in a string resource):
   stringResource(R.string.contacts_count, contactCount)  // %1$d handles locale-specific grouping
   ```

   **Note:** `%d` in Android string resources does NOT add thousands separators. If you want
   "1,808" vs "1808", use `NumberFormat` explicitly.

4. **Localize notification text in `TickleWorker.kt` and `TickleScheduler.kt`**

   Check how notification content is built. If the notification body includes user-facing text
   like "Time to reach out to John!", that string must use `context.getString(R.string.key)`.

   `TickleWorker` has access to `applicationContext` so `getString()` works directly.
   `TickleScheduler` may need a `Context` parameter if it builds notification strings.

   **Files:**
   - `service/TickleWorker.kt` — find all `.setContentTitle()` and `.setContentText()` calls
   - `service/TickleScheduler.kt` — check if it builds any display strings

5. **Add/update tests**

   In `StringResourceTest.kt`, add a test that verifies date formatting doesn't crash:
   ```kotlin
   @Test
   fun `date formatting respects locale`() {
       val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(
           java.time.format.FormatStyle.MEDIUM
       )
       val formatted = java.time.LocalDate.of(2026, 4, 11).format(formatter)
       assertTrue(formatted.isNotEmpty(), "Formatted date should not be empty")
   }
   ```

6. **Build and run all tests**

   ```bash
   cd android
   ./gradlew assembleDebug 2>&1 | tail -20
   ./gradlew testDebugUnitTest 2>&1 | tail -20
   ```

**Gotchas:**
- **`SimpleDateFormat` is NOT thread-safe.** If you find any shared instances, either make them
  `ThreadLocal` or replace with `java.time.format.DateTimeFormatter` (thread-safe).
- **Min SDK 26 means `java.time` is fully available** — no need for ThreeTenABP or desugaring.
- **Notification strings are evaluated when scheduled.** If the user changes locale between
  scheduling and receiving, the notification will be in the old locale. Acceptable trade-off.
- **`Locale.getDefault()` in unit tests** returns the JVM locale, which may differ from an
  Android device. Keep tests locale-independent or set `Locale.setDefault()` in test setUp.

**Scope:** `TickleListScreen.kt` (audit), `TickleWorker.kt` + `TickleScheduler.kt`
(notification strings), `StringResourceTest.kt`. Minimal code changes expected.

---

### ~~Task 0c — Phase 3: Add first non-English language (Spanish)~~ ✅ DONE (2026-04-12) — extended to 12 languages

**Prerequisite:** Task 0 (Phase 1) and Task 0b (Phase 2) must be complete.

**Goal:** Add full Spanish (`es`) translation to prove the i18n pipeline works end-to-end,
and confirm the app renders correctly in a non-English locale.

**Step-by-step:**

1. **Create the Spanish string resource file**

   Create `app/src/main/res/values-es/strings.xml` with translations for every key in the
   base `values/strings.xml`.

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <resources>
       <string name="app_name">Ticklr</string>

       <!-- Common -->
       <string name="common_save">Guardar</string>
       <string name="common_cancel">Cancelar</string>
       <string name="common_delete">Eliminar</string>
       <string name="common_done">Listo</string>
       <string name="common_edit">Editar</string>
       <string name="common_ok">Aceptar</string>
       <string name="common_reset">Restablecer</string>
       <string name="common_create">Crear</string>

       <!-- Settings -->
       <string name="settings_title">Ajustes</string>
       <string name="settings_section_appearance">Apariencia</string>
       <string name="settings_section_data">Datos</string>
       <string name="settings_section_messaging">Mensajería</string>
       <string name="settings_section_about">Acerca de</string>
       <string name="settings_section_developer">Desarrollador</string>
       <string name="settings_theme_title">Tema</string>
       <string name="settings_theme_system">Predeterminado del sistema</string>
       <string name="settings_theme_light">Claro</string>
       <string name="settings_theme_dark">Oscuro</string>
       <string name="settings_import_title">Importar contactos</string>
       <string name="settings_import_subtitle">Desde teléfono o CSV de LinkedIn</string>
       <string name="settings_templates_title">Plantillas de mensajes</string>
       <string name="settings_templates_subtitle">Crea y administra mensajes reutilizables</string>
       <string name="settings_sms_direct_title">Enviar SMS directamente</string>
       <string name="settings_sms_direct_subtitle">Usa SmsManager en lugar de abrir la app de Mensajes.</string>
       <string name="settings_about_privacy">Privacidad primero. Sin nube, sin analíticas, sin cuenta requerida. Todos los datos se almacenan en el dispositivo.</string>
       <string name="settings_about_built_by">Hecho por Xaymaca</string>
       <string name="settings_about_version">Versión %1$s</string>
       <string name="settings_reset_onboarding_title">Restablecer bienvenida</string>
       <string name="settings_reset_onboarding_subtitle">Mostrar la pantalla de bienvenida de nuevo</string>
       <string name="settings_reset_confirm_message">Esto te llevará a la pantalla de bienvenida. Tus datos no se eliminarán.</string>
       <string name="settings_clear_data_title">Borrar todos los datos</string>
       <string name="settings_clear_data_message">Esto eliminará permanentemente todos los contactos, grupos y tickles. No se puede deshacer.</string>
       <string name="settings_clear_data_confirm">Borrar todo</string>
       <string name="settings_choose_theme">Elegir tema</string>

       <!-- Frequencies -->
       <string name="frequency_daily">Diario</string>
       <string name="frequency_weekly">Semanal</string>
       <string name="frequency_biweekly">Cada 2 semanas</string>
       <string name="frequency_monthly">Mensual</string>
       <string name="frequency_bimonthly">Cada 2 meses</string>
       <string name="frequency_quarterly">Trimestral</string>
       <string name="frequency_custom">Personalizado</string>

       <!-- Tickle list -->
       <string name="tickle_list_title">Tickle</string>
       <string name="tickle_list_section_due">Vencidos</string>
       <string name="tickle_list_section_upcoming">Próximos</string>
       <string name="tickle_list_section_snoozed">Pospuestos</string>
       <string name="tickle_list_empty_title">Aún no hay tickles</string>
       <string name="tickle_list_empty_description">Agrega uno desde el detalle de un contacto, o toca +</string>
       <string name="tickle_row_today">Hoy</string>
       <string name="tickle_row_tomorrow">Mañana</string>
       <string name="tickle_row_yesterday">Ayer</string>

       <!-- ... translate ALL remaining keys from values/strings.xml ... -->
   </resources>
   ```

   **Important:** Every `<string>` and `<plurals>` key in `values/strings.xml` MUST have a
   corresponding entry in `values-es/strings.xml`. Missing keys will silently fall back to
   English, which creates a jarring mixed-language UI.

2. **Translate plural resources**

   Spanish pluralization is similar to English (one/other):
   ```xml
   <plurals name="tickle_row_days_overdue">
       <item quantity="one">%1$d día vencido</item>
       <item quantity="other">%1$d días vencido</item>
   </plurals>
   <plurals name="tickle_row_in_days">
       <item quantity="one">En %1$d día</item>
       <item quantity="other">En %1$d días</item>
   </plurals>
   <plurals name="contacts_count">
       <item quantity="one">%1$d contacto</item>
       <item quantity="other">%1$d contactos</item>
   </plurals>
   ```

3. **Do NOT translate these:**
   - "Ticklr" (brand name — keep `app_name` as "Ticklr" in Spanish too)
   - "Xaymaca" (company name)
   - Navigation route strings
   - SharedPreferences keys
   - Debug-only seed result messages

4. **Optionally add `resourceConfigurations` to `build.gradle.kts`**

   To limit which languages are bundled (useful for libraries that ship with 80+ locales):
   ```kotlin
   defaultConfig {
       resourceConfigurations += listOf("en", "es")
   }
   ```

5. **Test in Spanish locale**

   On emulator: `Settings → System → Languages → Español`
   Verify every screen:
   - [ ] Settings screen — all labels, section headers, dialogs
   - [ ] Tickle list — section headers, empty state, swipe actions
   - [ ] Tickle edit — all form labels, picker options, frequency names
   - [ ] Network list — search placeholder, empty state
   - [ ] Contact detail — all labels, button text, group bottom sheet
   - [ ] Group list + detail — all labels, toast messages
   - [ ] Compose — all labels, template picker, send button
   - [ ] Onboarding + Import — all copy
   - [ ] Launch screen — tagline (should stay English)

   **Automated locale test:** Add an instrumented test or use `Locale.setDefault(Locale("es"))`
   in a unit test setUp block to verify string resolution:
   ```kotlin
   @Test
   fun `spanish translations resolve correctly`() {
       // Note: This only works in instrumented tests (androidTest) where
       // Resources are available. For unit tests, verify file completeness instead.
   }
   ```

6. **Add a string completeness test**

   Create `app/src/test/java/com/xaymaca/sit/TranslationCompletenessTest.kt`:
   ```kotlin
   import org.junit.Test
   import java.io.File
   import kotlin.test.assertTrue

   class TranslationCompletenessTest {
       @Test
       fun `spanish strings xml has all keys from base strings xml`() {
           val baseFile = File("src/main/res/values/strings.xml")
           val esFile = File("src/main/res/values-es/strings.xml")
           assertTrue(esFile.exists(), "Spanish strings.xml should exist")

           val baseKeys = extractStringKeys(baseFile)
           val esKeys = extractStringKeys(esFile)
           val missing = baseKeys - esKeys
           assertTrue(missing.isEmpty(),
               "Spanish strings.xml is missing keys: ${missing.joinToString()}")
       }

       private fun extractStringKeys(file: File): Set<String> {
           val regex = Regex("""<string name="([^"]+)">""")
           return file.readLines()
               .mapNotNull { regex.find(it)?.groupValues?.get(1) }
               .toSet()
       }
   }
   ```

7. **Build and run all tests**

   ```bash
   cd android
   ./gradlew assembleDebug 2>&1 | tail -20
   ./gradlew testDebugUnitTest 2>&1 | tail -20
   ```

**Gotchas:**
- **Android selects language at the system level.** There's no in-app language picker by
  default. Android 13+ (API 33) supports per-app language preferences via
  `android.app.LocaleManager`, but the min SDK is 26. If you want an in-app language picker
  for older devices, use `AppCompatDelegate.setApplicationLocales()` from AndroidX AppCompat.
  This is a nice-to-have, not required for Phase 3.
- **String resource fallback is silent.** If `values-es/strings.xml` is missing a key, Android
  uses the base `values/strings.xml` value without warning. The completeness test above catches
  this at CI time.
- **Escape apostrophes and special characters** in XML: `\'` or wrap in `"double quotes"`.
- **Spanish strings may be ~20% longer than English.** Test for text truncation in tight layouts
  (buttons, chips, tab labels). Jetpack Compose handles this better than XML layouts, but
  verify visually.
- **`TicklrToast` text** — verify toast messages aren't truncated in Spanish.

**Scope:** Create `values-es/strings.xml` with all translations, optionally update
`build.gradle.kts`, add `TranslationCompletenessTest.kt`. No Kotlin code changes needed —
all wiring was done in Phase 1.

---

### Task 0d — Phase 4: RTL (Right-to-Left) language support (optional)

**Prerequisite:** Tasks 0, 0b, and 0c must be complete. Only needed if targeting Arabic, Hebrew,
Urdu, Farsi, or other RTL languages.

**Goal:** Ensure the app layout mirrors correctly for RTL locales and add the first RTL language.

**What needs to change:**

1. **Enable RTL support in `AndroidManifest.xml`**

   Add `android:supportsRtl="true"` to the `<application>` tag if not already present:
   ```xml
   <application
       android:supportsRtl="true"
       ...>
   ```
   This is required for any RTL rendering to work.

2. **Audit Compose layouts**

   Jetpack Compose handles RTL automatically for most standard components:
   - `Row` reverses child order in RTL
   - `padding(start = 16.dp)` maps to right side in RTL
   - `Arrangement.Start` maps to right in RTL
   - `Icon(Icons.AutoMirrored.Filled.Send)` already auto-mirrors (good — already in use)

   **Run the app in Arabic locale** (Emulator → Settings → Arabic) and screenshot every screen.
   Look for:

   - **Icons that should mirror** (back arrows, chevrons, send icons) — check if using
     `Icons.AutoMirrored.*` variants. If not, switch to them:
     ```kotlin
     // Bad — doesn't mirror:
     Icons.Default.ChevronRight
     // Good — mirrors in RTL:
     Icons.AutoMirrored.Filled.ChevronRight
     ```
   - **Icons that should NOT mirror:** `Icons.Default.Check`, `Icons.Default.Delete`,
     `Icons.Default.Add`, `Icons.Default.BugReport`, `Icons.Default.Palette`, the Ticklr logo
   - **`SettingsScreen.kt`** uses `Icons.Default.ChevronRight` in `SettingsRow` — this needs to
     become `Icons.AutoMirrored.Filled.ChevronRight` for RTL
   - **`Modifier.padding(start = X.dp)`** — verify all usages, especially in custom layouts.
     `start`/`end` is correct (auto-flips). Watch for `padding(left = X.dp)` — that's hardcoded
     and won't flip.

   **Search the codebase for these RTL-breaking patterns:**
   ```
   padding(left =
   padding(right =
   Arrangement.End        // usually fine, but verify context
   offset(x =             // hardcoded offsets don't flip
   absoluteOffset(        // explicitly non-flipping
   ```

3. **Fix `TicklrToast.kt`**

   The toast overlay may use hardcoded positioning. Verify it centers correctly in RTL.
   If it uses `Alignment.BottomCenter`, it's fine. If it uses absolute `x` offset, fix it.

4. **Test the launch screen**

   The animated Pulse logo should remain centered and not flip. The "YOUR PEOPLE MATTER" tagline
   should stay English and centered.

5. **Add Arabic translations**

   Create `app/src/main/res/values-ar/strings.xml` with Arabic translations for all keys.

   **Arabic has complex plural rules** — Android supports all six categories:
   ```xml
   <plurals name="tickle_row_days_overdue">
       <item quantity="zero">لا أيام متأخرة</item>
       <item quantity="one">يوم واحد متأخر</item>
       <item quantity="two">يومان متأخران</item>
       <item quantity="few">%1$d أيام متأخرة</item>
       <item quantity="many">%1$d يومًا متأخرًا</item>
       <item quantity="other">%1$d يوم متأخر</item>
   </plurals>
   ```

   Add `"ar"` to `resourceConfigurations` in `build.gradle.kts`.

6. **Add RTL-specific tests**

   In `TranslationCompletenessTest.kt`:
   ```kotlin
   @Test
   fun `arabic strings xml has all keys from base strings xml`() {
       val baseFile = File("src/main/res/values/strings.xml")
       val arFile = File("src/main/res/values-ar/strings.xml")
       assertTrue(arFile.exists(), "Arabic strings.xml should exist")

       val baseKeys = extractStringKeys(baseFile)
       val arKeys = extractStringKeys(arFile)
       val missing = baseKeys - arKeys
       assertTrue(missing.isEmpty(),
           "Arabic strings.xml is missing keys: ${missing.joinToString()}")
   }
   ```

   For RTL layout testing, use Compose UI tests:
   ```kotlin
   @Test
   fun settingsScreen_rendersInRTL() {
       composeTestRule.setContent {
           CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
               SettingsScreen(onImport = {}, onTemplates = {}, onResetOnboarding = {})
           }
       }
       composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
   }
   ```

7. **Build and run all tests**

   ```bash
   cd android
   ./gradlew assembleDebug 2>&1 | tail -20
   ./gradlew testDebugUnitTest 2>&1 | tail -20
   ```

**Gotchas:**
- **`android:supportsRtl="true"` must be in the manifest** or nothing mirrors. This is the
  most common missed step.
- **`start`/`end` vs `left`/`right`:** In Compose, `start`/`end` is the default and correct.
  In any XML layouts (if any remain), replace `paddingLeft`/`paddingRight` with
  `paddingStart`/`paddingEnd`.
- **Arabic text is significantly wider** than English — test for truncation in chips, buttons,
  and tab labels. `BottomNavigation` labels may need to be shorter in Arabic.
- **Mixed LTR/RTL text** (e.g. a contact named "John" in an Arabic UI) is handled by Android's
  bidirectional text algorithm. Numbers always stay LTR. Verify phone numbers display correctly.
- **The 30-character group name limit** may need adjustment for Arabic — Arabic characters are
  typically wider. Test visually.
- **This is the most labor-intensive phase.** Budget extra time for visual QA on every screen.
- **`Icons.AutoMirrored`** was added in `material-icons-extended` 1.5+ — verify the Compose
  BOM version includes it (it should, given the current dependencies).

**Scope:** `AndroidManifest.xml` (add `supportsRtl`), icon audit across all screen files,
`SettingsScreen.kt` (chevron icon fix), `values-ar/strings.xml`, `build.gradle.kts`,
`TranslationCompletenessTest.kt`. Most changes are config + translation — minimal Kotlin logic
changes.

---

Task files contain full diagnosis, root cause, and exact code changes. Read the task file
before making any changes.

| Task file | Type | What it covers |
|---|---|---|
| `tasks/fix-compose-bugs.md` | 🐛 Bug fix | (1) Direct SMS toggle ignored in ComposeScreen — pref read once at ViewModel init, never updated. (2) Contact search field loses focus when DropdownMenu opens — menu steals focus. Scoped to `ui/compose/ComposeScreen.kt` + `ComposeViewModel.kt`. |
| `tasks/feature-tickle-empty-groups-prompt.md` | ✨ Feature | Show empty state + inline "Create a Group" dialog when Group tab has no groups in `TickleEditScreen`. Scoped to `ui/tickle/TickleEditScreen.kt` only. |
| `tasks/feature-unique-group-names.md` | ✨ Feature | Prevent duplicate group names at all creation and rename entry points (GroupListScreen, GroupDetailScreen, ContactDetailScreen, TickleEditScreen). Adds `isGroupNameTaken()` to `GroupViewModel`. |

**Recommended execution order:** fix-compose-bugs → feature-tickle-empty-groups-prompt → feature-unique-group-names (the unique-names task references the dialog added by the empty-groups task).

---

## ✅ Feature: ContactDetailScreen — Add Group Button + Compose Button (complete)

### What to Build

Add two action buttons to `ContactDetailScreen` immediately below the existing
"Add Tickle" button:

---

#### Button 1: Add to Group

**Label:** `"Add to Group"` with `Icons.Default.Group`, Cobalt color
**Behavior:**
- Tapping opens a `ModalBottomSheet` (or `AlertDialog`) listing all existing groups
- Each row: group name with a checkmark `Icons.Default.Check` if contact is already a member
- Tapping a row toggles membership — add if not member, remove if already a member
- At the bottom of the list: a `"+ Create New Group"` row that expands an inline
  `OutlinedTextField` + `"Create"` button
- On create: insert new group, add contact to it, collapse the inline field
- Max 30 char group name, Create button disabled when blank

**ViewModel changes needed in `NetworkViewModel`:**
- `fun getGroupsForContact(contactId: Long): Flow<List<ContactGroup>>`
  (already exists in `ContactRepository` — just expose it)
- `fun getAllGroups(): Flow<List<ContactGroup>>`
  (already exists — just expose it in NetworkViewModel or use a shared GroupViewModel)
- `fun addContactToGroup(contactId: Long, groupId: Long)`
- `fun removeContactFromGroup(contactId: Long, groupId: Long)`
- `fun createGroupAndAddContact(groupName: String, contactId: Long)`

**Note:** `ContactRepository` already has `addContactToGroup` and `removeContactFromGroup`.
Expose them via `NetworkViewModel` or inject `GroupViewModel` into the screen.

---

#### Button 2: Message

**Label:** `"Message"` with `Icons.AutoMirrored.Filled.Send`, Cobalt color
**Behavior:**
- Only enabled if the contact has at least one phone number
- On tap: immediately opens SMS intent pre-populated with `contact.phoneNumbers.first`
  and an empty body — bypasses ComposeScreen entirely for speed
- Use `SmsService.sendSmsIntent(context, listOf(phone), "")` which opens the Messages app
- If no phone number: button is greyed out, show `"No phone number on file"` tooltip or
  just rely on the existing empty state already shown in ContactDetailScreen

**No new ViewModel needed** — fire the intent directly from the composable using `LocalContext`.

---

### Button Layout

Replace the current single "Add Tickle" Button with this layout:

```
┌─────────────────────┐  ┌─────────────────────┐
│  🔔 Add Tickle      │  │  👥 Add to Group    │
└─────────────────────┘  └─────────────────────┘
┌──────────────────────────────────────────────┐
│              → Message                        │
└──────────────────────────────────────────────┘
```

Use a `Row` for the first two buttons (equal weight with `Modifier.weight(1f)`),
then a full-width Button for Message below. Tickle stays Amber. Both new buttons use Cobalt.

**Also fix:** `ContactDetailScreen` currently uses Gson `TypeToken` to parse phone/email/tags
— replace with the R8-safe `parseJsonStringArray()` helper (same pattern as ComposeScreen).
This is the same R8 crash risk documented in the critical fix section above.

---

## 🎨 REDESIGN: ComposeScreen — Single Contact + Template Dropdown

### Design Goals
Current `ComposeScreen` is too busy. New design:
- **One contact at a time** — no multi-select, no checkbox list
- **Template dropdown only** — no save/create template on this screen
- **Template CRUD** moves to dedicated screens under Settings
- **Contact search with dropdown results** — no scrollable LazyColumn
- Focused layout that works with keyboard open

### New ComposeScreen Layout (top to bottom)

```
┌────────────────────────────────────────┐
│  Compose                         [TopBar]
├────────────────────────────────────────┤
│  To: [Search contacts field]           │  OutlinedTextField, single line
│      [Selected contact chip + X]       │  shown below field when contact selected
├────────────────────────────────────────┤
│  [Template dropdown — only if          │  OutlinedButton "Select template ▾"
│   templates exist, hidden otherwise]   │  selecting auto-fills message body
├────────────────────────────────────────┤
│  Message                               │
│  ┌──────────────────────────────────┐  │
│  │  OutlinedTextField (min 120dp)   │  │
│  └──────────────────────────────────┘  │
├────────────────────────────────────────┤
│                       [Send button →]  │  disabled until contact + message set
└────────────────────────────────────────┘
```

### Contact Search Behavior
- Typing in "To:" filters contacts by name/company in real time
- Results appear as `DropdownMenu` anchored below field (not a LazyColumn)
- Tapping a result: selects contact, clears search text, shows chip, moves focus to message
- Chip shows full name + X button to clear and re-search
- If selected contact has no phone number: inline warning "No phone number on file"

### Template Dropdown Behavior
- Hidden entirely if `templates.isEmpty()`
- Selecting a template populates `messageBody` — user can still edit after
- Label: "Select template" when none, template title when selected

### Send Button
- Enabled only when: contact selected AND has phone number AND message not blank
- Respects `sendDirectly` pref — SmsManager or Intent
- After send: clear contact + message, show TicklrToast "Message sent ✓"
- **Remove all multi-recipient logic** — single contact only

---

### ComposeViewModel Changes

**Remove:**
- `selectedContactIds: MutableStateFlow<Set<Long>>`
- `toggleContactSelection(contactId: Long)`
- `clearSelection()`
- `SortOrder` enum, `sortOrder` StateFlow, `setSortOrder()`
- `saveTemplate()` — moves to `TemplateViewModel`
- `deleteTemplate()` — moves to `TemplateViewModel`

**Add:**
- `selectedContact: MutableStateFlow<Contact?>` — single selected contact
- `fun selectContact(contact: Contact)` — sets contact, clears searchQuery
- `fun clearContact()` — clears selectedContact
- `fun clearCompose()` — clears selectedContact + messageBody
- `val canSend: StateFlow<Boolean>` — contact != null && phones not empty && message not blank

**Keep:** `searchQuery`, `setSearchQuery()`, `contacts` (filtered, no sort), `templates`,
`messageBody`, `setMessage()`, `applyTemplate()`, `sendDirectly`, `toastMessage`, `clearToast()`

---

### New Template Management Screens (under `ui/settings/`)

#### `TemplateListScreen.kt`
- `LazyColumn` of all templates — title + 2-line body preview
- Swipe to delete (same SwipeToDismiss pattern as TickleListScreen)
- FAB "+" → navigate to `TemplateEditScreen(templateId = -1)`
- Empty state: "No templates yet. Tap + to create one."
- Seeds default "Checking in" template on first launch via SharedPrefs flag
  `hasSeededDefaultTemplates` (mirror iOS behavior)

#### `TemplateEditScreen.kt`
- Two fields: `Title` (single line) + `Body` (multiline min 80dp)
- Save/Update button in TopBar — disabled until both fields non-blank
- Pre-populate when editing existing template
- On save: call ViewModel → popBackStack

#### `TemplateViewModel.kt`
```kotlin
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repo: MessageTemplateRepository
) : ViewModel() {
    val templates: StateFlow<List<MessageTemplate>>
    fun saveTemplate(title: String, body: String)
    fun updateTemplate(template: MessageTemplate)
    fun deleteTemplate(template: MessageTemplate)
    fun seedDefaultIfNeeded(prefs: SharedPreferences)
}
```

---

### Navigation Changes

**`Screen.kt` — add:**
```kotlin
object TemplateList : Screen("template_list")
data class TemplateEdit(val id: Long = -1L) : Screen("template_edit/{templateId}") {
    companion object {
        const val ROUTE = "template_edit/{templateId}"
        fun createRoute(id: Long = -1L) = "template_edit/$id"
    }
}
```

**`NavGraph.kt` — add composables for TemplateList and TemplateEdit**

**`SettingsScreen.kt` — add `onTemplates: () -> Unit` param + SettingsRow in Data section**

---

### Files to Create
- `ui/settings/TemplateListScreen.kt`
- `ui/settings/TemplateEditScreen.kt`
- `ui/settings/TemplateViewModel.kt`

### Files to Modify
- `ui/compose/ComposeScreen.kt` — full redesign
- `ui/compose/ComposeViewModel.kt` — per changes above
- `ui/nav/Screen.kt` — add TemplateList + TemplateEdit
- `ui/nav/NavGraph.kt` — add template composables, update SettingsScreen call
- `ui/settings/SettingsScreen.kt` — add onTemplates param + row

### Files to Leave Alone
- `data/model/MessageTemplate.kt`, `data/dao/MessageTemplateDao.kt`,
  `data/repository/MessageTemplateRepository.kt`, all other screens

---

## Architecture — MVVM + Repository
- **UI**: Jetpack Compose (no XML layouts)
- **State**: ViewModel + StateFlow
- **Persistence**: Room (version 2) + TypeConverters for List<String>
- **DI**: Hilt
- **Navigation**: Navigation Compose (NavGraph.kt)
- **Background work**: WorkManager (TickleWorker)
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 35

## Project Structure

```
app/src/main/java/com/xaymaca/sit/
├── SITApp.kt                       # @HiltAndroidApp, PREFS_NAME, KEY_ONBOARDING_COMPLETE
├── MainActivity.kt                 # Entry point, NavGraph host
├── data/
│   ├── model/
│   │   ├── Contact.kt              # Room @Entity
│   │   ├── ContactGroup.kt         # Room @Entity
│   │   ├── ContactGroupCrossRef.kt # Many-to-many join table
│   │   ├── MessageTemplate.kt      # Room @Entity
│   │   ├── TickleReminder.kt       # Room @Entity
│   │   ├── Enums.kt                # ImportSource, TickleFrequency, TickleStatus
│   │   └── Relations.kt            # ContactWithGroups, GroupWithContacts
│   ├── dao/
│   │   ├── ContactDao.kt
│   │   ├── ContactGroupDao.kt
│   │   ├── MessageTemplateDao.kt
│   │   └── TickleReminderDao.kt
│   ├── db/
│   │   └── SITDatabase.kt          # Room DB, version 2, StringListConverter
│   └── repository/
│       ├── ContactRepository.kt
│       ├── MessageTemplateRepository.kt
│       └── TickleRepository.kt
├── di/
│   └── DatabaseModule.kt           # Hilt module — provides DB and DAOs
├── service/
│   ├── ContactImportService.kt     # ContactsContract import
│   ├── LinkedInCSVParser.kt        # CSV parsing (mirrors iOS implementation)
│   ├── SmsService.kt               # SmsManager direct send + Intent fallback
│   ├── StringListConverter.kt      # Room TypeConverter for List<String> — Gson-free, R8-safe
│   ├── TickleScheduler.kt          # nextDueDate logic + WorkManager scheduling
│   └── TickleWorker.kt             # WorkManager Worker for notifications
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # Navy, Cobalt, Amber + variants
│   │   ├── Theme.kt                # SITTheme (dark-first, Material3)
│   │   └── Type.kt                 # Typography
│   ├── nav/
│   │   ├── NavGraph.kt             # Full NavHost + BottomNavigation
│   │   └── Screen.kt              # Sealed class for all routes
│   ├── launch/
│   │   └── LaunchScreen.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   └── ImportScreen.kt
│   ├── network/
│   │   ├── NetworkListScreen.kt    # Searchable contact list + reachability icons
│   │   ├── NetworkViewModel.kt
│   │   ├── ContactDetailScreen.kt
│   │   └── AddContactScreen.kt     # Create + edit (reused with contactId param)
│   ├── groups/
│   │   ├── GroupListScreen.kt
│   │   ├── GroupDetailScreen.kt
│   │   └── GroupViewModel.kt
│   ├── tickle/
│   │   ├── TickleListScreen.kt     # Due/Upcoming/Snoozed sections
│   │   ├── TickleEditScreen.kt
│   │   └── TickleViewModel.kt
│   ├── compose/
│   │   ├── ComposeScreen.kt
│   │   └── ComposeViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── shared/
│       ├── TagChipRow.kt
│       └── TicklrToast.kt          # Shared toast overlay (Cobalt bg, white text, 12dp radius)
└── tests/
    ├── LinkedInCSVParserTest.kt    # Unit tests — passing
    ├── StringListConverterTest.kt  # Unit tests — passing
    └── TickleSchedulerTest.kt      # Unit tests — passing
```


## What's Complete ✅ — Full Android Feature Set

- Room database — all 5 entities, 4 DAOs, StringListConverter, version 2
- Hilt DI — DatabaseModule providing all repositories
- Compose theme — Navy/Cobalt/Amber Pulse identity, dark-first Material3
- Full NavGraph — 5-tab bottom nav + all nested routes with proper back stack
- `NetworkListScreen` — searchable contact list with trailing reachability icons (phone/email)
- `ContactDetailScreen` — full detail view with tickle shortcut
- `AddContactScreen` — create + edit (reused via contactId param)
- `GroupListScreen` + `GroupDetailScreen`
- `GroupDetailScreen` — add-member toast: `"Name added to Group"` / `"Name added to group"` (truncates >20 chars); 30-char group name limit with live counter in both create and edit dialogs
- `TickleListScreen` — Due/Upcoming/Snoozed sections; swipe-to-complete resets row via `dismissState.reset()` in `LaunchedEffect`
- `TickleEditScreen` — create/edit with contact/group picker, frequency, date, note; contact search clears on selection; save/update shows `"Tickle saved"` / `"Tickle updated"` toast
- `TickleScheduler` — nextDueDate logic + WorkManager scheduling
- `TickleWorker` — WorkManager Worker for background notifications
- `ComposeScreen` + `ComposeViewModel` — single-contact redesign; contact search with DropdownMenu chip; template dropdown (hidden if empty); canSend StateFlow; "Message sent ✓" toast after send
- `TemplateListScreen` + `TemplateEditScreen` + `TemplateViewModel` — full template CRUD under Settings; swipe-to-delete; seeds default "Checking in" template on first launch; accessed via "Message Templates" row in Settings
- `ContactDetailScreen` — "Add Tickle" + "Add to Group" buttons in Row, full-width "Message" button below; `ModalBottomSheet` for group membership toggle + inline "Create New Group" (30-char limit); Gson TypeToken replaced with R8-safe `parseJsonStringArray()`
- `SmsService` — SmsManager direct send + Intent fallback
- `ImportScreen` — LinkedIn CSV + contacts import; LinkedIn section framed as relationship seeding with correct copy
- `ContactImportService` — ContactsContract import
- `LinkedInCSVParser` — mirrors iOS implementation
- `SettingsScreen` + `SettingsViewModel` — includes debug "Load Test Contacts" button
- `SeedDataService` — DEBUG only, loads `test_contacts.csv` from assets
- `LaunchScreen` — Pulse identity splash
- `OnboardingScreen`
- `TicklrToast` — shared `Box`-overlay toast composable in `ui/shared/`; used by GroupDetailScreen, ComposeScreen, TickleEditScreen
- `ContactFingerprint` — Gson-free, R8-safe; uses `parseJsonStringArray()` instead of TypeToken
- `StringListConverter` — Gson-free, R8-safe; plain string parser for `fromString`, manual serializer for `fromList`
- Unit tests — LinkedInCSVParser, StringListConverter, TickleScheduler, ScreenRoute, ContactRepository (all passing)
- Build artifacts present — app has been compiled and built successfully
- Screenshot prep — `./gradlew screenshotPrep` sets 9:41, full signal, 100% battery, no notifications
- Screenshot teardown — `./gradlew screenshotTeardown` restores normal status bar

### Android — Nice to Have
- **SmsManager direct send UX** — surface the "send directly vs open Messages" preference in Settings

- **Contact filter in `AddMemberSheet`** — The bottom sheet that opens when adding members to a group (`GroupDetailScreen.kt`, composable `AddMemberSheet` ~line 249) has no search/filter field. Users with large contact lists have to scroll to find someone. iOS already has this via `.searchable()` on its `NavigationStack`. Add a filter text field to Android to match.

  **Implementation — all changes are inside `AddMemberSheet`:**
  1. Add state: `var searchQuery by remember { mutableStateOf("") }`
  2. Derive filtered list: `val filteredContacts = contacts.filter { it.displayName.contains(searchQuery, ignoreCase = true) }`
  3. Insert an `OutlinedTextField` between the "Add Member" title `Text` and the `LazyColumn`:
     ```kotlin
     OutlinedTextField(
         value = searchQuery,
         onValueChange = { searchQuery = it },
         placeholder = { Text("Filter contacts…") },
         leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
         singleLine = true,
         shape = RoundedCornerShape(24.dp),
         modifier = Modifier
             .fillMaxWidth()
             .padding(horizontal = 16.dp, vertical = 8.dp)
     )
     ```
  4. Pass `filteredContacts` instead of `contacts` to the `LazyColumn`'s `items()` call.
  5. `searchQuery` resets naturally when the sheet is dismissed (scoped to composable lifetime) — verify this is true in practice and add an explicit `LaunchedEffect` reset if needed.

  **iOS reference:** `ios/Sources/SIT/Views/Network/GroupDetailView.swift` — `AddMemberSheet` uses `@State private var searchText` filtered against `displayName`, applied via `.searchable()` on the `NavigationStack`. Case-insensitive match on `displayName`.

---

## Key Notes

- `SITApp.PREFS_NAME` + `SITApp.KEY_ONBOARDING_COMPLETE` — SharedPreferences keys for onboarding state
- Room DB is version 2 — any schema changes need a migration
- `ContactGroupCrossRef` handles the many-to-many Contact ↔ Group relationship
- `StringListConverter` serializes `List<String>` for phone numbers, emails, tags — Gson-free as of v1.4.4
- Android can send SMS silently via `SmsManager` with SEND_SMS permission — iOS cannot
- WorkManager handles tickle notifications — persists across reboots
- `AddContactScreen` doubles as edit screen via optional `contactId` parameter

## Pulse Brand in Compose

```kotlin
val Navy   = Color(0xFF0A1628)   // backgrounds
val Cobalt = Color(0xFF2563EB)   // primary actions
val Amber  = Color(0xFFF5C842)   // accent, tickle due state
```

## Permissions (AndroidManifest)
- `READ_CONTACTS` — contacts import
- `SEND_SMS` — direct SMS (runtime request, graceful fallback to Intent)
- `POST_NOTIFICATIONS` — tickle reminders (Android 13+)

## Build & Run
Open `android/` in Android Studio (Hedgehog or newer).
Run on API 26+ emulator or physical Android device.
SMS direct-send requires physical device with active SIM.

## Sensitive Files — Never Commit
`keystore.jks`, `*.keystore`, `release.properties`, `google-services.json`,
any file containing signing passwords or API keys.
