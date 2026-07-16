---
name: ios-release
description: >-
  Cut a Ticklr iOS release end to end — decide the version, write + localize
  App Store release notes (20 locales), bump the version, land it on main,
  tag ios/v* to trigger the Xcode Cloud build, then hand off the manual App
  Store Connect steps. Use when asked to "cut/ship/release the iOS app" or
  tag an ios/v* version. Covers the gotchas: pbxproj staging, editable ASC
  version, fastlane metadata push. iOS only.
---

# Cut an iOS Release

End-to-end iOS release. The build is driven by a git tag: pushing
`ios/v<version>` on `main` triggers the **Xcode Cloud** workflow (configured
in App Store Connect, NOT in this repo — no YAML to read).
`ios/ci_scripts/ci_post_clone.sh` patches `MARKETING_VERSION` from the tag,
so the tag is the version of record.

Unlike Android, the tag is **not** the full deploy: the build lands in
TestFlight/ASC, and shipping still requires manual App Store Connect steps
(attach build → Submit for Review) that only Vincent can do.

## Step 1 — Pre-flight: what's shipping, and as what version?

```bash
git tag -l 'ios/v*' | sort -V | tail -1                                  # last release tag
git log --oneline "$(git tag -l 'ios/v*' | sort -V | tail -1)"..HEAD     # unreleased commits
```

Read the unreleased commits, filter to iOS-relevant ones, and propose the
version (semver: minor for features, patch for fixes). Confirm with the user
unless they already named it.

## Step 2 — Release notes (20 App Store locales)

Notes live at `fastlane/metadata/<locale>/release_notes.txt` — 20 locales
(en-US source; **no `ur`** — iOS ASC metadata is 20 locales, not the app's 21).

1. **Re-read every current file first** — the #1 failure mode is shipping the
   previous release's notes.
2. Rewrite `en-US` for what's actually shipping: one lead line + `•` bullets.
3. Translate to the other 19, matching each locale's existing register
   (informal tu/du/ты across the board) and keeping brand vocabulary:
   `Ticklr`, `YOUR PEOPLE MATTER` untranslated; **"tickle" stays invariant**
   (lowercase in most locales; German capitalizes "Tickle"; Swedish declines
   "tickeln"; ro hyphenates "tickle-ul/-urile"). Mirror the bullet structure
   across locales.
4. Validate: `python3 scripts/validate-app-store-metadata.py` (from repo
   root; checks all ASC field limits — notes cap is 4000 chars, rarely
   binding).

## Step 3 — Version bump

- `ios/project.yml` → `MARKETING_VERSION: "<x.y.z>"`, then
  `cd ios && xcodegen generate` and commit the regenerated pbxproj.
- Bump the Android `versionName` fallback in `android/app/build.gradle.kts`
  too (per `docs/RELEASING.md`, keep versions aligned — harmless because
  nothing ships on Play without an `android/v*` tag).

**pbxproj landmines** (both bit real releases):
- Stage with `git add -f ios/SIT.xcodeproj/project.pbxproj` — an unanchored
  `SIT.xcodeproj` gitignore rule silently no-ops a plain `git add`.
- Diff before committing: only `MARKETING_VERSION` lines should change, and
  strip any non-empty `DEVELOPMENT_TEAM = <ID>` hunks (empty `""` is fine).

## Step 4 — Land prep on main

Branch → PR → merge (never tag a branch; never stack PRs). Stage only
release-relevant files; keep unrelated working-tree changes out.

```bash
git switch -c chore/ios-v<x.y.z>-release
git add fastlane/metadata/*/release_notes.txt ios/project.yml android/app/build.gradle.kts
git add -f ios/SIT.xcodeproj/project.pbxproj
git commit -m "chore: prep iOS v<x.y.z> release — notes (20 locales) + version bump"
# open PR, wait for CI, merge, then sync local main
```

## Step 5 — Tag (triggers the Xcode Cloud build)

```bash
git tag ios/v<x.y.z> <merge-commit>
git push origin ios/v<x.y.z>
```

Watch Xcode Cloud progress in App Store Connect (nothing to watch in GitHub
Actions — only the unit-test workflow runs there).

## Step 6 — Manual ASC steps (Vincent)

1. In App Store Connect: create version `<x.y.z>` on the app page ("+").
2. Attach the Xcode Cloud build; Submit for Review.
3. Push metadata/notes: suggest `! ~/bin/release_notes.sh` — Vincent's
   wrapper that validates metadata, exports the ASC API-key env vars (key is
   held personally; never in repo/CI), and runs `fastlane ios store_metadata`.
   **Gotcha:** deliver needs an *editable* ASC version to exist — if it loops
   on "Cannot find edit app store version… retrying", do step 1 first.
4. Verify the store page once live; update Linear (shipped issues → Done).

## Quick reference — gotchas

- Tag = version of record; `ci_post_clone.sh` deliberately avoids
  `git describe` (it once resolved an older tag on the same commit).
- Metadata is 20 locales (no `ur`); Play is 21 — don't copy locale lists
  between platforms.
- `git add` on the pbxproj silently no-ops without `-f`.
- Never commit `DEVELOPMENT_TEAM` IDs, `.p8`/`.p12`/`.mobileprovision`, or
  `~/bin/release_notes.sh`'s contents.
- `fastlane ios store_metadata` is metadata-only (Deliverfile skips binary,
  screenshots, review submission) and requires an editable ASC version.
