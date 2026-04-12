# CLAUDE.md — Ticklr (iOS)

---

## 🛠️ Pending Tasks — Start Here

### Task 0 ✅ COMPLETE — Phase 1 Internationalization: Extract all hardcoded strings to String Catalog

**Goal:** Every user-visible string in the iOS app must come from a localized source so that
adding a new language later is a translation-only task — no code changes required.

**Approach — Use Xcode String Catalogs (`.xcstrings`)**

iOS 17+ supports String Catalogs natively. Since our min target is iOS 17.0, use this approach
instead of legacy `Localizable.strings` files. String Catalogs give us compiler-checked keys,
pluralization support, and Xcode's built-in translation editor for free.

**Step-by-step:**

1. **Create the String Catalog file**
   - Add `Sources/SIT/Resources/Localizable.xcstrings` (Xcode type: "String Catalog")
   - Add the file to `project.yml` under the Ticklr target sources if needed — XcodeGen should
     pick it up automatically from the `Sources/SIT` source path, but verify after generation.
   - Set development language to English (`en`).

2. **Extract strings from every SwiftUI view file**

   Replace every hardcoded `"string"` in SwiftUI `Text()`, `Label()`, `Button()`,
   `.navigationTitle()`, `Section()`, `.confirmationDialog()`, `ContentUnavailableView()`,
   `Toggle()`, `Picker()`, and `LabeledContent()` calls with `String(localized:)` keys.

   **Naming convention for keys:** Use dot-separated, lowercase, descriptive keys that include
   the screen context. Examples:
   ```
   "settings.section.data"              → "Data"
   "settings.row.importContacts"        → "Start Your Network"
   "settings.row.messageTemplates"      → "Message Templates"
   "settings.about.appName"             → "Ticklr"
   "settings.about.builtBy"             → "Built by Xaymaca"
   "tickleList.section.due"             → "Due"
   "tickleList.section.upcoming"        → "Upcoming"
   "tickleList.section.snoozed"         → "Snoozed"
   "tickleList.empty.title"             → "No tickles yet"
   "tickleList.empty.description"       → "Add one from a contact's detail page, or tap +"
   "common.save"                        → "Save"
   "common.cancel"                      → "Cancel"
   "common.delete"                      → "Delete"
   "common.done"                        → "Done"
   "common.edit"                        → "Edit"
   "common.ok"                          → "OK"
   ```

   **Pattern — before:**
   ```swift
   Text("No tickles yet")
   ```

   **Pattern — after:**
   ```swift
   Text(String(localized: "tickleList.empty.title"))
   ```

   For strings with interpolation, use `String(localized:)` with a default value:
   ```swift
   // Before:
   Text("Loaded \(result.imported) test contacts ✓")
   // After:
   Text(String(localized: "debug.seedResult.loaded \(result.imported)",
               defaultValue: "Loaded \(result.imported) test contacts ✓"))
   ```

   **Files to modify (all in `Sources/SIT/Views/`):**
   - `Settings/SettingsView.swift` (~25 strings)
   - `Settings/TemplateListView.swift`
   - `Settings/TemplateEditView.swift`
   - `Tickle/TickleListView.swift` (~10 strings)
   - `Tickle/TickleRowView.swift` (relative date labels: "Today", "Yesterday", "Xd overdue", etc.)
   - `Tickle/TickleEditView.swift` (~15 strings)
   - `Network/NetworkListView.swift`
   - `Network/ContactRowView.swift`
   - `Network/ContactDetailView.swift` (~20 strings)
   - `Network/AddContactView.swift`
   - `Network/GroupListView.swift`
   - `Network/GroupDetailView.swift`
   - `Compose/ComposeView.swift`
   - `Onboarding/OnboardingView.swift`
   - `Onboarding/ImportView.swift` (~15 strings)
   - `App/ContentView.swift` (tab labels)
   - `App/LaunchScreenView.swift` ("YOUR PEOPLE MATTER" tagline)

3. **Localize relative date strings in `TickleRowView.swift`**

   The view currently builds relative dates with hardcoded English like `"Today"`, `"Yesterday"`,
   `"3d overdue"`, `"In 2d"`. Replace these with `String(localized:)` keys that use string
   interpolation for the numbers. Example keys:
   ```
   "tickleRow.due.today"                → "Today"
   "tickleRow.due.yesterday"            → "Yesterday"
   "tickleRow.due.overdue \(days)"      → "%lld d overdue"
   "tickleRow.due.upcoming \(days)"     → "In %lld d"
   ```

4. **Localize `TickleFrequency` enum display names**

   The `TickleFrequency` enum's `rawValue` strings (e.g. "Weekly", "Monthly") are shown in
   pickers and labels. Add a `localizedName` computed property:
   ```swift
   var localizedName: String {
       switch self {
       case .daily:   return String(localized: "frequency.daily")
       case .weekly:  return String(localized: "frequency.weekly")
       case .monthly: return String(localized: "frequency.monthly")
       case .custom:  return String(localized: "frequency.custom")
       }
   }
   ```
   Then replace all UI references from `freq.rawValue` to `freq.localizedName`.

5. **Do NOT localize these:**
   - `#if DEBUG` sections (seed messages, debug buttons) — these are developer-only
   - Model property names or database keys
   - SF Symbol names
   - Brand name "Ticklr" when used as the app name (keep as literal)
   - The tagline "YOUR PEOPLE MATTER" — keep as brand, do not localize

6. **Run `xcodegen generate` after adding the `.xcstrings` file**

7. **Add unit tests for localization coverage**

   Create `Tests/SITTests/LocalizationTests.swift` with tests that verify:
   - The String Catalog file exists in the bundle
   - Key sample strings resolve to non-empty values (catch missing keys)
   - Strings with interpolation format correctly
   ```swift
   import XCTest
   @testable import Ticklr

   final class LocalizationTests: XCTestCase {
       func testKeyStringsAreNotEmpty() {
           let keys = [
               "settings.section.data",
               "tickleList.section.due",
               "common.save",
               "common.cancel",
               "onboarding.welcome.title"
           ]
           for key in keys {
               let localized = String(localized: String.LocalizationValue(key))
               XCTAssertFalse(localized.isEmpty, "Localized string for '\(key)' should not be empty")
               // If key == localized, the key was not found in the catalog
               XCTAssertNotEqual(localized, key, "Key '\(key)' was not found in String Catalog")
           }
       }
   }
   ```

8. **Verify the app builds and all existing + new tests pass**

   ```bash
   cd ios
   xcodegen generate
   xcodebuild test \
     -project SIT.xcodeproj \
     -scheme Ticklr \
     -destination 'platform=iOS Simulator,name=iPhone 16' \
     -resultBundlePath TestResults \
     2>&1 | tail -30
   ```

**Gotchas & constraints:**
- **Do not break the running app.** Every `String(localized:)` call must have a corresponding
  entry in the `.xcstrings` catalog with the English value, otherwise the UI will show raw keys.
  Work file-by-file: extract strings → add to catalog → build → verify before moving to the next.
- **SwiftUI `Text` accepts `LocalizedStringKey` by default.** When you write `Text("Hello")`,
  SwiftUI already treats it as a localization key. However, we want *explicit* keys (not the
  English string as the key) for maintainability. Use `Text(String(localized: "key"))` so that
  keys are stable even if English copy changes.
- **`Section("header")` and `.navigationTitle("title")` also accept `LocalizedStringKey`.** Same
  rule applies — use explicit keys via `String(localized:)`.
- **Pluralization:** For strings like "X contacts", use String Catalog's built-in plural rules
  rather than manual `if count == 1` checks.
- **The `project.yml` does not currently list `CFBundleLocalizations`.** You may need to add
  `en` to the Info.plist properties in `project.yml`:
  ```yaml
  CFBundleDevelopmentRegion: en
  CFBundleLocalizations:
    - en
  ```
- **Do NOT add any new languages yet.** This task is English-only string extraction.
  Adding languages (e.g. Spanish, French) is Phase 2 — a separate task.

**Scope:** All SwiftUI view files + `TickleFrequency` enum + new test file. No model changes,
no service changes, no navigation changes, no new dependencies.

---

### Task 0b ✅ COMPLETE — Phase 2: Locale-aware date/time and number formatting

**Prerequisite:** Task 0 (Phase 1) must be complete — all UI strings extracted to String Catalog.

**Goal:** Ensure all dates, times, and numbers displayed in the app respect the user's locale
settings, so that when translations are added in Phase 3, dates render correctly (e.g.
"11 avr. 2026" in French vs "Apr 11, 2026" in English).

**What needs to change:**

1. **Audit `.formatted()` calls — most are already locale-aware**

   Swift's `.formatted(date: .abbreviated, time: .omitted)` on `Date` already respects the
   device locale. Verify this is the case in:
   - `TickleRowView.swift` line 39: `reminder.nextDueDate.formatted(date: .abbreviated, time: .omitted)`
   - `TickleEditView.swift` — any date picker labels or date display
   - `TickleListView.swift` — section headers if they show dates

   **Action:** Run the app in Simulator with locale set to French (`Settings → General →
   Language & Region → Region → France`) and verify dates render in French format. If they
   do, no code changes needed — just document this as verified.

2. **Replace manual relative date logic with `RelativeDateTimeFormatter`**

   `TickleRowView.swift` currently computes relative dates manually ("Today", "Yesterday",
   "3d overdue", "In 2d") using `Calendar.current` and `String(localized:)` keys. This works
   for simple translations, but `RelativeDateTimeFormatter` would give us:
   - Proper locale-aware phrasing (e.g. "hace 3 días" in Spanish, "il y a 3 jours" in French)
   - Automatic pluralization across all languages
   - Less maintenance — Apple handles the grammar

   **However**, the current manual approach gives us more UI control (short labels like "3d overdue"
   vs the formatter's "3 days ago"). **Decision point:**

   **Option A (Recommended — keep manual, it's already localized):**
   The Phase 1 `String(localized:)` keys with interpolation already handle this. The String
   Catalog supports plural variants per key, so `"tickleRow.due.overdue \(days)"` can have
   different forms for `one` vs `other` in each language. No code change needed — just ensure
   the String Catalog entries have plural variants configured.

   **Option B (Use RelativeDateTimeFormatter):**
   ```swift
   private var dueDateLabel: String {
       let formatter = RelativeDateTimeFormatter()
       formatter.unitsStyle = .abbreviated  // "2 days ago" → "2d ago"
       formatter.dateTimeStyle = .named     // "today", "yesterday"
       return formatter.localizedString(for: reminder.nextDueDate, relativeTo: Date())
   }
   ```
   Downside: less control over exact wording, "overdue" framing lost (it just says "2 days ago").

   **Go with Option A** unless there's a specific reason to switch.

3. **Verify number formatting**

   `SettingsView.swift` uses `contacts.count.formatted()` which is already locale-aware
   (renders "1,808" in English, "1 808" in French, "1.808" in German). Verify in Simulator.

4. **Localize notification text in `TickleScheduler.swift`**

   Check how notification content strings are built in `TickleScheduler.scheduleNotification()`.
   If the notification body includes user-facing text like "Time to reach out to John!", that
   string must use `String(localized:)` too. Notification content created via
   `UNMutableNotificationContent` runs outside SwiftUI but `String(localized:)` works in any
   Swift context.

   **Files:**
   - `Services/TickleScheduler.swift` — find all `.body = "..."` and `.title = "..."` assignments

5. **Add/update tests**

   Add a test in `LocalizationTests.swift` that verifies date formatting respects locale:
   ```swift
   func testDateFormattingRespectsLocale() {
       let date = Date(timeIntervalSince1970: 0) // Jan 1, 1970
       let formatted = date.formatted(date: .abbreviated, time: .omitted)
       // Just verify it returns a non-empty string — exact format depends on test environment locale
       XCTAssertFalse(formatted.isEmpty)
   }
   ```

6. **Build and run all tests**

   ```bash
   cd ios && xcodegen generate
   xcodebuild test -project SIT.xcodeproj -scheme Ticklr \
     -destination 'platform=iOS Simulator,name=iPhone 16' 2>&1 | tail -30
   ```

**Gotchas:**
- `RelativeDateTimeFormatter` output varies by OS version — test on iOS 17.0 specifically
- The `Date.formatted()` API respects `Locale.current` automatically — do NOT hardcode a locale
- Notification strings are evaluated at schedule time, not delivery time. If the user changes
  their language between scheduling and receiving, the notification will be in the old language.
  This is an acceptable trade-off — don't over-engineer it.

**Scope:** `TickleRowView.swift` (audit only if Option A), `TickleScheduler.swift` (notification
strings), `LocalizationTests.swift`. Minimal code changes expected.

---

### Task 0c ✅ COMPLETE — Phase 3: Add first non-English language (Spanish)

**Prerequisite:** Task 0 (Phase 1) and Task 0b (Phase 2) must be complete.

**Goal:** Add full Spanish (`es`) translation to prove the i18n pipeline works end-to-end,
and confirm the app renders correctly in a non-English locale.

**Step-by-step:**

1. **Add Spanish to the String Catalog**

   Open `Sources/SIT/Resources/Localizable.xcstrings` and add `es` (Spanish) as a supported
   language. Xcode's String Catalog editor allows adding languages directly — it will create
   blank entries for every existing key.

   Alternatively, from the command line / code: the `.xcstrings` file is JSON under the hood.
   Each key has a `localizations` dict. Add an `"es"` entry for each key:
   ```json
   "settings.section.data": {
       "localizations": {
           "en": { "stringUnit": { "state": "translated", "value": "Data" } },
           "es": { "stringUnit": { "state": "translated", "value": "Datos" } }
       }
   }
   ```

2. **Translate all keys**

   Provide Spanish translations for every key in the catalog. Group them logically:

   **Common:**
   | Key | English | Spanish |
   |-----|---------|---------|
   | `common.save` | Save | Guardar |
   | `common.cancel` | Cancel | Cancelar |
   | `common.delete` | Delete | Eliminar |
   | `common.done` | Done | Listo |
   | `common.edit` | Edit | Editar |
   | `common.ok` | OK | Aceptar |
   | `common.reset` | Reset | Restablecer |
   | `common.create` | Create | Crear |

   **Frequencies:**
   | Key | English | Spanish |
   |-----|---------|---------|
   | `frequency.daily` | Daily | Diario |
   | `frequency.weekly` | Weekly | Semanal |
   | `frequency.biweekly` | Every 2 weeks | Cada 2 semanas |
   | `frequency.monthly` | Monthly | Mensual |
   | `frequency.bimonthly` | Every 2 months | Cada 2 meses |
   | `frequency.quarterly` | Quarterly | Trimestral |
   | `frequency.custom` | Custom | Personalizado |

   **Do NOT translate these:**
   - "Ticklr" (brand name)
   - "Xaymaca" (company name)
   - "YOUR PEOPLE MATTER" (brand tagline — keep English)

   For the full set (~188 keys), translate every remaining string. Use a professional
   translator or translation service for production quality. For initial implementation,
   Claude can generate draft translations that should be reviewed by a native speaker.

3. **Configure plural variants for Spanish**

   Spanish pluralization is similar to English (one/other), but verify that keys with
   interpolated counts have proper plural forms in the String Catalog:
   ```json
   "tickleRow.due.overdue %lld": {
       "localizations": {
           "es": {
               "variations": {
                   "plural": {
                       "one": { "stringUnit": { "value": "%lld día vencido" } },
                       "other": { "stringUnit": { "value": "%lld días vencido" } }
                   }
               }
           }
       }
   }
   ```

4. **Update `project.yml` to declare supported localizations**

   ```yaml
   info:
     properties:
       CFBundleDevelopmentRegion: en
       CFBundleLocalizations:
         - en
         - es
   ```

   Run `xcodegen generate` after this change.

5. **Test in Spanish locale**

   In Simulator: `Settings → General → Language & Region → Español`
   Verify every screen:
   - [ ] Settings screen — all labels, section headers, dialogs
   - [ ] Tickle list — section headers, empty state, swipe actions
   - [ ] Tickle edit — all form labels, picker options, frequency names
   - [ ] Network list — search placeholder, empty state
   - [ ] Contact detail — all labels, button text, group sheet
   - [ ] Group list + detail — all labels, toast messages
   - [ ] Compose — all labels, template picker, send button
   - [ ] Onboarding + Import — all copy
   - [ ] Launch screen — tagline (should stay English)

6. **Add Spanish locale test**

   In `LocalizationTests.swift`:
   ```swift
   func testSpanishTranslationsExist() {
       let bundle = Bundle(for: type(of: self))  // or Bundle.main in app target
       let esBundle = Bundle(path: bundle.path(forResource: "es", ofType: "lproj")!)
       XCTAssertNotNil(esBundle, "Spanish localization bundle should exist")

       // Spot-check a few keys
       let save = NSLocalizedString("common.save", bundle: esBundle!, comment: "")
       XCTAssertEqual(save, "Guardar")
   }
   ```

   **Note:** With String Catalogs, testing specific locale bundles may work differently than
   with `.strings` files. If `Bundle(path:)` doesn't work for `.xcstrings`, use an alternative
   approach: set `UserDefaults` `AppleLanguages` to `["es"]` in a test setUp and verify
   `String(localized:)` returns Spanish strings.

7. **Build and run all tests**

   ```bash
   cd ios && xcodegen generate
   xcodebuild test -project SIT.xcodeproj -scheme Ticklr \
     -destination 'platform=iOS Simulator,name=iPhone 16' 2>&1 | tail -30
   ```

**Gotchas:**
- **String Catalogs are JSON.** They can be edited programmatically, but be careful not to
  corrupt the structure. Prefer Xcode's editor if possible.
- **App Store requires all supported languages to be complete.** Don't ship a half-translated
  app — every key must have a Spanish value before release.
- **Date/number formatting follows the device's Region setting**, not the Language setting.
  A user can have Language=Spanish but Region=United States — dates will still be in US format.
  This is correct Apple behavior, don't fight it.
- **Xcode previews** default to the development language. To preview in Spanish, add
  `.environment(\.locale, Locale(identifier: "es"))` to preview providers.

**Scope:** `Localizable.xcstrings` (add all Spanish translations), `project.yml` (add `es` to
CFBundleLocalizations), `LocalizationTests.swift` (add Spanish tests). No Swift code changes
needed — all wiring was done in Phase 1.

---

### Task 0d — Phase 4: RTL (Right-to-Left) language support (optional)

**Prerequisite:** Tasks 0, 0b, and 0c must be complete. Only needed if targeting Arabic, Hebrew,
Urdu, Farsi, or other RTL languages.

**Goal:** Ensure the app layout mirrors correctly for RTL locales and add the first RTL language.

**What needs to change:**

1. **Audit layout direction behavior**

   SwiftUI handles RTL automatically for most standard components:
   - `HStack` reverses child order in RTL
   - `NavigationStack` flips back buttons
   - `List` and `Form` flip row layout
   - `.padding(.leading)` maps to right side in RTL

   **Run the app in Arabic locale** (Simulator → Settings → Arabic) and screenshot every
   screen. Look for:
   - Text alignment issues (any hardcoded `.leading` that should stay `.leading`)
   - Icons that should mirror (arrows, chevrons) vs icons that should NOT mirror (checkmarks,
     clock faces, the Ticklr logo)
   - `HStack` layouts that look wrong when reversed

2. **Fix SF Symbol mirroring**

   Some SF Symbols auto-mirror in RTL, others don't. For symbols that represent directional
   actions, ensure they mirror:
   ```swift
   Image(systemName: "chevron.right")
       .flipsForRightToLeftLayoutDirection(true)  // if not auto-mirroring
   ```

   Symbols that should NOT mirror: `checkmark`, `bell`, `plus`, `trash`, `pencil`, brand logo.

3. **Fix any hardcoded layout**

   Search for these patterns that break in RTL:
   - `HStack` with `.frame(alignment: .leading)` — should use `.leading` (OK, it flips)
   - `.padding(.trailing, 16)` — fine, it flips
   - Hardcoded `x` offsets in `.offset()` or `.position()` — these do NOT flip automatically
   - `Text` with explicit `.multilineTextAlignment(.leading)` — this is OK, `.leading` flips
   - Any `CGAffineTransform` or manual frame calculations

4. **Test the launch screen and onboarding**

   The animated Pulse logo and "YOUR PEOPLE MATTER" tagline in `LaunchScreenView.swift` should
   remain centered and not flip. The onboarding flow should read naturally in RTL.

5. **Add Arabic (or Hebrew) translations**

   Follow the same process as Phase 3:
   - Add `ar` to the String Catalog
   - Translate all ~188 keys
   - Add `ar` to `CFBundleLocalizations` in `project.yml`
   - Arabic has complex plural rules (zero, one, two, few, many, other) — configure all
     plural variants in the String Catalog

6. **Add RTL-specific tests**

   ```swift
   func testLayoutDirectionRTL() {
       // Verify the app doesn't crash when launched in RTL
       let view = ContentView()
           .environment(\.layoutDirection, .rightToLeft)
           .environment(\.locale, Locale(identifier: "ar"))
       // Snapshot test or just verify it renders without errors
       let _ = try? view.inspect()  // if using ViewInspector
   }
   ```

7. **Build and run all tests in RTL simulator**

   ```bash
   cd ios && xcodegen generate
   # Run with RTL pseudolanguage to catch layout issues:
   xcodebuild test -project SIT.xcodeproj -scheme Ticklr \
     -destination 'platform=iOS Simulator,name=iPhone 16' \
     -testLanguage ar -testRegion SA \
     2>&1 | tail -30
   ```

**Gotchas:**
- **Portrait-only lock is fine for RTL.** No orientation changes needed.
- **`Text(verbatim:)` does not flip** — it renders the string as-is. This is correct for
  phone numbers, URLs, and code. Verify `Text(verbatim: dueDateLabel)` in `TickleRowView`
  still looks right in RTL (numbers should stay LTR even in RTL context — this is standard).
- **Arabic text is significantly wider** than English in some fonts — test for truncation.
- **SwiftUI's `.searchable()` modifier** already handles RTL text input. No changes needed.
- **This is the most labor-intensive phase.** Budget extra time for visual QA on every screen.

**Scope:** Visual audit of all screens, SF Symbol mirroring fixes, Arabic translations in
String Catalog, `project.yml` update, new tests. Minimal Swift code changes — mostly
SwiftUI auto-handles RTL.

---

These are parity issues found by comparing against the Android implementation. Both affect real UX.

### Task 1 — Empty group state in `TickleEditView` (Group picker)

**File:** `Views/Tickle/TickleEditView.swift`

**Problem:** When no groups exist and the user switches to the Group segment in `TickleEditView`,
the native `Picker` renders only "Choose a group" with nothing selectable. There is no empty state
message and no way to create a group without abandoning the screen (losing all form state).

**Fix:** Detect `allGroups.isEmpty` inside the Group branch of the `Who` section.
Instead of the `Picker`, show:
- A brief message: `"No groups yet"` + subtext `"Groups let you tickle everyone on a team or in a circle at once."`
- A `Button("Create a Group")` that presents `GroupEditSheet(group: nil)` as a sheet
- After the sheet dismisses, if a group was created it will appear in `allGroups` (driven by `@Query`) and the Picker can then be shown

No new state needed beyond `@State private var showingCreateGroupSheet = false`.
`@Query(sort: \ContactGroup.name) private var allGroups` is already present.

**Scope:** `TickleEditView.swift` only — no ViewModel, no navigation changes.

---

### Task 2 — Duplicate group names allowed

**Files:** `Views/Network/GroupListView.swift`, `Views/Network/ContactDetailView.swift`

**Problem:** `GroupEditSheet.canSave` only checks `!name.isEmpty && name.count <= 30`.
No uniqueness check exists at any group creation or rename entry point:
1. `GroupListView` → `+` button → `GroupEditSheet(group: nil)` — create, no dupe check
2. `GroupListView` → swipe Edit → `GroupEditSheet(group: group)` — rename, no dupe check
3. `ContactDetailView` → `AddToGroupSheet` inline create field — `Create` button only checks non-blank

**Fix:**

`GroupEditSheet` already receives `group: ContactGroup?` (nil = create, non-nil = edit).
It needs a way to query existing group names for the uniqueness check.

Add `@Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]` to `GroupEditSheet`
and derive a `isDuplicate` flag:

```swift
private var isDuplicate: Bool {
    let trimmed = name.trimmingCharacters(in: .whitespaces)
    guard !trimmed.isEmpty else { return false }
    return allGroups.contains {
        $0.name.caseInsensitiveCompare(trimmed) == .orderedSame &&
        $0.id != group?.id  // allow rename to same name
    }
}
```

Update `canSave`:
```swift
private var canSave: Bool {
    !name.trimmingCharacters(in: .whitespaces).isEmpty &&
    name.count <= 30 &&
    !isDuplicate
}
```

Show error text below the name field when `isDuplicate`:
```swift
if isDuplicate {
    Text("A group with this name already exists")
        .font(.caption)
        .foregroundStyle(.red)
}
```

For `AddToGroupSheet` inline create in `ContactDetailView`:
- Derive `isDuplicateInline` the same way using the existing `allGroups` `@Query`
- Disable the `Create` button when `isDuplicateInline`
- Show `Text("Name already exists").font(.caption).foregroundStyle(.red)` below the field

**Scope:** `GroupListView.swift` (`GroupEditSheet`) + `ContactDetailView.swift` (`AddToGroupSheet`).

---

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
