# TIC-110: Duo SDK and simulator baseline

Validation date: October 29–30, 2026. Implementation branch:
`codex/tic-110-duo-baseline`, based on main `1b64ebf`.
[Review PR](https://github.com/vinny2020/ticklr/pull/139).

## Configuration and archive evidence

The fresh unsigned local archive succeeded with Xcode 27.1 (27A9275), SDK
`iphoneos27.1` (24A94232). Assertions against the archived app's Info.plist
confirm minimum iOS 17.0, device families `[1,2]`, iPhone portrait and both
landscapes, and all four iPad orientations. No full-screen or scene-resize
compatibility opt-out was present in the archived app, generated project,
XcodeGen source, or xcconfig overrides (none exist).

XcodeGen 2.45.4 regenerated the project and shared scheme. Project metadata
now says 27.1. The Cloud post-clone script records the effective SDK and
rejects versions below 27.1. Shell syntax, plist assertions, and diff whitespace
checks passed.

## Local simulator validation

`bash scripts/ios/validate-duo.sh` completed with exit code 0 on the isolated
branch: 244 unit tests and 7 UI tests passed on iPhone Duo / iOS 27.1. The
selected UI suites cover baseline launch, Compose cancellation, contact tickle
create/edit/cancel, and canonical group navigation. Tutorial screenshot
production suites are excluded from this regression command.

Evidence retained locally:

- `logs/duo/validation-20261029-233053-15854.log`
- Results and archive: `$TMPDIR/ticklr-duo.j0C6Y1/`
- `logs/duo/archive-configuration.json`
- `logs/duo/screenshots-duo/` (exported XCTest attachments and manifest)
- Representative portrait captures: [Duo](tic-110-screenshots/duo-portrait.png),
  [iPhone](tic-110-screenshots/iphone-portrait.png),
  [iPad](tic-110-screenshots/ipad-portrait.png)

The baseline test requests portrait and both landscape orientations,
then checks that native navigation remains available and attaches screenshots.
The Duo runtime returned identical portrait-shaped captures for all three
requests. These prove launch and continued navigation availability; they do
not establish that display geometry actually rotated. Real fold/pose/rotation
behavior remains acceptance work in TIC-111/112/115.

Standard iPhone 17 and iPad Pro 11-inch (M5), both iOS 27.0: the baseline
launch test passed on both destinations (28.086 s and 57.695 s respectively).
The run returned TEST SUCCEEDED; results are in
`logs/duo/StandardBaseline.xcresult`, with raw output in
`logs/duo/standard-baseline.log` and six exported screenshots in
`logs/duo/screenshots-standard/`.

## CI and Cloud

[GitHub Actions run](https://github.com/vinny2020/ticklr/actions/runs/40997994442)
uses the documented `xcode-27` runner and explicitly selects
`/Applications/Xcode_27.1.app`. It archives unsigned, runs all unit tests and
core Duo UI tests, and runs baseline launches on standard iPhone/iPad.
Raw logs and both result bundles are uploaded as `ios-test-results`.
[GitHub's runner inventory](https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md)
confirmed these Xcode and simulator destinations on the validation date.

In Xcode → Integrate → Manage Workflows → Ticklr → CICD → Environment,
the existing Xcode Cloud selector is **Latest Release**, which its menu
explicitly resolves to **Xcode 27.1 (27A9275)**. The macOS selector also uses
Latest Release. No Cloud release build was triggered. An explicit version pin
was rejected by automatic approval review; the verified alias already meets
the requirement, so no configuration change was needed. The checked-in SDK
guard protects future Cloud builds after merge.

## Apple App Resizability assessment

Exported Apple's installed skill with
`xcrun agent skills export --output-dir /private/tmp/tic110-apple-skills`.
Read its prerequisites and all five task references, scanned production
sources, and independently verified the findings.

- Launch screen declaration is present in generated/source/archived plist.
- iPad supports all four orientations; full-screen opt-out is absent.
- No production `UIScreen.main` / `mainScreen` usage.
- No orientation-based layout decisions or deprecated scene geometry callback.
- SwiftUI `App` / `WindowGroup` lifecycle; no AppDelegate migration applies.
- No device-idiom layout branching.
- Safe-area assessment found potential double-insetting in
  `OnboardingView.swift` (geometry inset padding and height subtraction).
  Review and fix under TIC-112.

Other TIC-112 review sites: `ImportView.swift` applies an unscoped
`ignoresSafeArea()` to a native Contacts picker; Compose retains fixed
196-point margins at regular width; Tickle retains a calculated sheet height.
These findings are recorded for layout implementation, beyond this baseline.

This establishes the toolchain/configuration baseline. Physical-device QA,
iOS 17 runtime regression, full resizability, and release assets are gated by
TIC-109's later sub-issues.

## CI failure investigation

The first remote run archived successfully and passed unit/Compose/Groups
coverage, but the contact tickle UI test failed to find Save after typing.
Downloaded its result bundle, UI hierarchy, and recording: the software
keyboard displaced the editor toolbar in the short Duo presentation. The
local run had not exposed that keyboard state. Added an explicit Done submit
label and focus dismissal to the note field; the UI test now submits the note
before Save/Cancel, exercising the real keyboard dismissal route. The editor
retains the entered text. The next CI run validates this correction.

The subsequent remote run stopped advancing inside the editor unit test's
full-form Save search after adding focus/submit modifiers. Local editor tests
still passed but took 8.6 seconds. Extracted the exact toolbar Save view as an
internal property, following Compose's existing approach, and inspect that
action directly in the eight wiring tests. Rendered toolbar, keyboard, and
persistence behavior remain covered by the contact UI flow.
