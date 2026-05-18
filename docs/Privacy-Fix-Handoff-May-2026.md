# Ticklr Privacy Fix Handoff — May 2026

> Audit done by Claude (chat) against `~/Projects/ticklr` on 2026-05-17 after the warm-redesign photo work landed. The photo feature itself is sound — these tasks close gaps that mostly predate it but become material now that user-attached photos exist on disk.

## Context & Constraints

- **Privacy promise (from `assets/design-system/project/warm-redesign/HANDOFF.md`):** read-only on the address book, nothing leaves the device, no writes back to the system Contacts DB.
- **What's already correct (do NOT change):**
  - iOS `ContactImportService.swift` and Android `ContactImportService.kt` — neither imports photo data.
  - iOS `Contact.swift` SwiftData model + Android `Contact.kt` Room entity — neither has a photo field.
  - iOS `PhotoStore.swift` already flags files with `isExcludedFromBackup = true`.
  - iOS `ContactPhotoFetcher.swift` + Android `ContactPhotoService.kt` — read-only against system Contacts, in-memory cache only, gated on permission grant.
  - Single-contact deletion already wipes the user-attached photo on both platforms (`ContactDetailView.swift` line ~74, `ContactDetailScreen.kt` line ~356).
  - The "Add a photo" affordance uses non-permission system pickers on both sides (`PhotosPicker` on iOS, `PickVisualMedia` on Android). No new permission prompt was introduced.
- **The new photo permission Vince was worried about doesn't actually exist.** Photo picking goes through the modern non-permission system pickers. The contacts permission is the same one the import flow already uses.

---

## Task 1 — Android: disable auto-backup of personal data ⚠️ CRITICAL

**The problem.** `android/app/src/main/AndroidManifest.xml` has `android:allowBackup="true"` and no `xml/data_extraction_rules.xml` or `xml/full_backup_content.xml`. There is no `app/src/main/res/xml/` directory at all. Result: the Room DB and `filesDir/photos/` are eligible for Google Drive auto-backup on Android 6+. This directly contradicts "no cloud, no server communication" in the Play Store listing.

**The fix.** Take the surgical approach (option 2 from the audit) — keep `allowBackup="true"` but exclude personal data, so user prefs can still survive a reinstall while contacts/tickles/photos cannot. If Vince prefers the iOS-matching posture (no backup at all), flip `allowBackup="false"` instead and skip the XML files.

### 1a. Create `android/app/src/main/res/xml/data_extraction_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Android 12+ (API 31+) backup/transfer rules. Excludes all personal data
  from both cloud backup (Google Drive) and device-to-device transfer.
-->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" />
        <exclude domain="file" path="photos/" />
        <exclude domain="sharedpref" path="onboarding_prefs.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" />
        <exclude domain="file" path="photos/" />
    </device-transfer>
</data-extraction-rules>
```

### 1b. Create `android/app/src/main/res/xml/full_backup_content.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Android 11 and below (API 30-) backup rules. -->
<full-backup-content>
    <exclude domain="database" />
    <exclude domain="file" path="photos/" />
    <exclude domain="sharedpref" path="onboarding_prefs.xml" />
</full-backup-content>
```

### 1c. Wire into the manifest

In `android/app/src/main/AndroidManifest.xml`, update the `<application>` tag:

```xml
<application
    android:name=".SITApp"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/full_backup_content"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.SIT.Splash">
```

### Verification

1. Build: `./gradlew :app:assembleDebug` from `android/`.
2. Install on a device with Google backup enabled.
3. Trigger backup: `adb shell bmgr backupnow com.xaymaca.sit`.
4. Pull and inspect: `adb shell dumpsys backup`. DB and `photos/` should not appear.
5. Verify the Room DB filename in `data/db/AppDatabase.kt` matches what would land under `domain="database"`.

### Done when

- [ ] Both XML files exist under `app/src/main/res/xml/`.
- [ ] Manifest references them.
- [ ] Verified via `adb` that backup omits DB and photos.
- [ ] If `onboarding_prefs.xml` isn't the actual SharedPreferences filename, update the `sharedpref` path or drop that line.

---

## Task 2 — Android: "Clear All Data" must wipe photo files

**The problem.** `SettingsViewModel.clearAllContacts()` (lines 58–68 of `app/src/main/java/com/xaymaca/sit/ui/settings/SettingsViewModel.kt`) wipes the DB but never touches `filesDir/photos/`. Every attached contact photo stays on disk after "All data cleared ✓".

**The fix.**

```kotlin
fun clearAllContacts() {
    viewModelScope.launch {
        try {
            contactRepository.deleteAllContacts()
            contactRepository.deleteAllGroups()
            tickleRepository.deleteAllReminders()
            WorkManager.getInstance(context).cancelAllWork()

            // Wipe user-attached contact photos from filesDir/photos/.
            // Single-contact deletion already handles this in ContactDetailScreen;
            // this branch is the bulk equivalent.
            withContext(Dispatchers.IO) {
                File(context.filesDir, "photos").deleteRecursively()
            }
            // Drop the in-memory bitmap cache too so the UI doesn't show
            // a stale photo for a contact ID that gets reused.
            ContactPhotoService.clearCache()

            _seedMessage.value = "All data cleared ✓"
        } catch (e: Exception) {
            _seedMessage.value = "Clear failed: ${e.message}"
        }
    }
}
```

Imports:

```kotlin
import com.xaymaca.sit.service.ContactPhotoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
```

### Verification

1. Attach a photo to a contact via Contact Detail in the debug build.
2. `adb shell run-as com.xaymaca.sit ls files/photos/` — confirm the .jpg.
3. Settings → Debug → Clear All Data.
4. Re-run the `ls` — directory gone or empty.

---

## Task 3 — iOS: parity for "Clear All Data" (debug-only today, fix anyway)

**The problem.** `ios/Sources/SIT/Views/Settings/SettingsView.swift` lines ~164–185 wipe SwiftData but never touch `Library/Application Support/Ticklr/ContactPhotos/`. The block is `#if DEBUG` today so users don't hit it, but Vince debug-tests against real data and this will graduate to a production "Reset App" feature.

**The fix.** After the SwiftData deletes, before `seedMessage = "All data cleared ✓"`:

```swift
// Wipe user-attached photos.
let support = FileManager.default.urls(for: .applicationSupportDirectory,
                                       in: .userDomainMask)[0]
let photosURL = support.appendingPathComponent("Ticklr/ContactPhotos",
                                               isDirectory: true)
try? FileManager.default.removeItem(at: photosURL)

// Drop the in-memory cache so a reused UUID can't surface a stale image.
ContactPhotoFetcher.clearCache()

UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
try? modelContext.save()
seedMessage = "All data cleared ✓"
```

**Cleaner factoring (preferred):** add a helper to `PhotoStore.swift`:

```swift
static func deleteAll() {
    try? FileManager.default.removeItem(at: folderURL)
}
```

Then call `PhotoStore.deleteAll()` from `SettingsView` instead of duplicating the path logic. Photo storage concerns belong in `PhotoStore`.

### Verification

1. Attach a photo via Contact Detail in the debug scheme.
2. Inspect simulator container: `Library/Application Support/Ticklr/ContactPhotos/` — .jpg should be present.
3. Settings → Clear All Data → Delete All Data.
4. Directory gone or empty.

---

## Task 4 — iOS: add `PrivacyInfo.xcprivacy` (Privacy Manifest)

**The problem.** Apple requires a Privacy Manifest for apps using "required reason APIs". Ticklr uses `UserDefaults` (via `@AppStorage`) and `URLResourceValues` (`PhotoStore` setting `isExcludedFromBackup`). No `PrivacyInfo.xcprivacy` exists today. Surfaces as warning/rejection on next App Store submission.

**The fix.** Create `ios/Sources/SIT/Resources/PrivacyInfo.xcprivacy`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array/>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>CA92.1</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>C617.1</string>
            </array>
        </dict>
    </array>
</dict>
</plist>
```

Reason codes:
- `CA92.1` — `UserDefaults`: access info from same app group/app (this is `@AppStorage` for `hasCompletedOnboarding`, etc.)
- `C617.1` — File timestamp: inside-app file management (`PhotoStore` setting `isExcludedFromBackup`)

### Wire into the Xcode project

1. **Preferred:** add `Resources/PrivacyInfo.xcprivacy` to the `sources:` list for the SIT target in `ios/project.yml`, then `xcodegen generate` from `ios/`.
2. Alternative: drag into Xcode SIT target resources.

Verify it appears in the built `.app`:

```sh
cd ios
xcodebuild -project SIT.xcodeproj -scheme SIT -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 15' build
find ~/Library/Developer/Xcode/DerivedData -name PrivacyInfo.xcprivacy -path '*SIT.app*'
```

### Done when

- [ ] File exists at `ios/Sources/SIT/Resources/PrivacyInfo.xcprivacy`.
- [ ] Listed in `project.yml` (or added in Xcode).
- [ ] `xcodegen generate` runs clean.
- [ ] Built `.app` contains the file at the bundle root.
- [ ] Quick grep for additional required-reason APIs: `grep -rE "(systemUptime|fileModificationDate|attributesOfItem|volumeAvailableCapacity)" ios/Sources/SIT` — investigate any hits.

---

## Task 5 — Listing copy sweep (no code; flag for Vince)

Once Tasks 1–4 are merged, the App Store and Play Store data-safety sections need a review pass:

- **Photo access** — note photos can be optionally attached from the library via the system picker, stored locally, never transmitted. System Contacts photo, if present, is read on-the-fly and never persisted.
- **Contacts access** — confirm existing copy still reflects "read-only, never written back."
- **Data Safety (Play)** — confirm "no data collected, no data shared" stays accurate after Task 1.

CC: don't edit store-listing files (`ios/docs/app-store-listing.md`, `android/app/src/main/play/release-notes/`) — flag back to Vince.

---

## Suggested commit / branch strategy

- `feat/privacy-hardening-android` — Tasks 1, 2 (two commits)
- `feat/privacy-hardening-ios` — Tasks 3, 4 (two commits)

Commit message example:

> Android: exclude DB and photos from auto-backup
>
> Closes the auto-backup gap identified in the May 2026 privacy audit.
> See docs/Privacy-Fix-Handoff-May-2026.md Task 1.

---

## Out of scope

- Photo encryption at rest — iOS Data Protection (Class C) and Android FBE are sufficient for v1.
- Crash reporting — none exists, right call for privacy-first, do NOT add Crashlytics/Sentry/etc.
- Encrypted database (SQLCipher, encrypted SwiftData) — out of scope; threat model assumes user controls device encryption.
