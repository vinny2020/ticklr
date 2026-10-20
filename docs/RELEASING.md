# Releasing Ticklr

How a release gets from `main` to the App Store and Google Play. Both pipelines
are **tag-driven**: pushing a tag is the deploy button. Everything before the
tag is ordinary PR work; everything after is automated (Android) or
semi-automated (iOS).

Most recent audit: [1.15.1](releases/1.15.1-audit.md), 19 October 2026.
Check the recorded store states before announcing a joint public release.

## The flow at a glance

```
1. Land all release work on main (PRs, never stacked)
2. Write + localize release notes  ──►  PR
3. Version bump                    ──►  PR
4. Tag the release commit:
     android/v<x.y.z>-production   ──►  GitHub Actions builds AAB → publishes to Play
     ios/v<x.y.z>                  ──►  Xcode Cloud builds → TestFlight/ASC
5. iOS: create editable version, upload metadata/screenshots, attach passed build
6. Enable the build for the intended TestFlight group; verify IN_BETA_TESTING
7. Submit the App Store version for review; verify the public store after approval
```

---

## Android — GitHub Actions

**Pipeline:** `.github/workflows/android-release.yml`, triggered by any tag
matching `android/v*`.

**Tag format:** `android/v<x.y.z>-<track>` where track is `production`,
`beta`, or `alpha`. A bare `android/v<x.y.z>` goes to the `internal` track.
The tag selects version and track only — **tag annotations are ignored**.

What the workflow does:

1. Parses version name + track from the tag.
2. Verifies the tag's version matches the `versionName` literal in
   `android/app/build.gradle.kts` and reads `versionCode` from the same file.
   Both are **plain literals bumped by PR before tagging** (the version-bump
   PR increments `versionCode` by 1 and sets the new `versionName`). They must
   be regex-able literals because F-Droid parses the build file at the tagged
   commit and cannot evaluate Gradle expressions (TIC-101). Tagging without
   landing the bump PR fails the workflow before anything builds or uploads.
3. **Validates checked-in release notes** for the tagged track: every
   `android/app/src/main/play/release-notes/<locale>/<track>.txt` must be
   ≤ 500 *characters* (counted with `wc -m` in a UTF-8 locale, not bytes).
   Over-limit notes fail the build before anything uploads.
4. Decodes the signing keystore and Play service account from repo secrets
   (`KEYSTORE_BASE64`, `PLAY_SERVICE_ACCOUNT_JSON`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`).
5. Runs `./gradlew publishReleaseBundle` — builds the signed AAB and publishes
   it straight to the Play track. **A production tag publishes to production
   with no further confirmation step.**
6. Builds the signed release **APK** and attaches it to a GitHub Release named
   after the tag (`ticklr-v<x.y.z>.apk`, non-production tracks marked
   pre-release). F-Droid's reproducible-build flow (`Binaries:` in fdroiddata)
   downloads this APK, verifies their from-source build matches it, and
   publishes our dev-signed APK — so this artifact must exist for every
   production release (TIC-101).

**Scope caveat:** `publishReleaseBundle` ships the bundle + release notes only.
The store **listing** (title, descriptions, screenshots, contact/privacy URLs)
is not touched. Publish repository listing changes separately with
`./gradlew :app:publishReleaseListing` from `android/`. Validate a draft first
with `-PplayCommit=false`; listing edits affect the live listing across tracks.

### Android release notes

Written per-locale as checked-in files, 21 locale dirs for production. Full
authoring/translation procedure lives in the
`.claude/skills/android-release-notes` skill; batch translation fallback is
`scripts/prep-release-notes.py <track>` (needs `$OPENAI_API_KEY`).

Non-negotiables:

- `en-US/production.txt` is the source; every locale mirrors its bullet
  structure. Keep English ≤ ~350 chars so expansive locales (de, ru, el) stay
  under 500.
- **Hebrew is `iw-IL` only — never add `he-IL`.** The Play API normalizes both
  to the same locale and having both fails the release *commit* with
  `400 — duplicates` at the very end of the pipeline (broke the v1.9.0 cut;
  Linear TIC-42).
- Brand strings stay untranslated: `Ticklr`, `Xaymaca`, `YOUR PEOPLE MATTER`,
  and usually "Tickle".
- Re-read the current files before writing — the #1 failure mode is shipping
  the *previous* release's notes.

### Android signing (local reference)

Release keystore: `~/Documents/ticklr-release.keystore` (alias `ticklr`),
credentials in `android/local.properties`. Never commit keystores,
`release.properties`, or `google-services.json` (see `android/CLAUDE.md`).

---

## iOS — Xcode Cloud

**Pipeline:** configured in App Store Connect (Xcode Cloud), **not** in this
repo — there is no workflow YAML to read. It triggers on tags matching
`ios/v*`.

Step-by-step checklist (notes, bump, tag, ASC handoff) lives in the
`.claude/skills/ios-release` skill — the iOS counterpart to
`android-release`.

The only in-repo piece is `ios/ci_scripts/ci_post_clone.sh`, which Xcode Cloud
runs after cloning:

- Resolves the version from the triggering tag (`CI_TAG`), falling back to the
  highest `ios/v*` tag on HEAD via `sort -V`. (Deliberately not `git describe`
  — it picks by graph distance and annotated-over-lightweight, which once
  resolved an older tag on the same commit.)
- Patches `MARKETING_VERSION` directly into `SIT.xcodeproj/project.pbxproj`.

So the tag is the version of record; the pbxproj value is a fallback. Keep
`ios/project.yml`'s `MARKETING_VERSION` in sync anyway (the version-bump PR)
so local builds agree.

**App Store Connect steps after the build** (UI or authenticated ASC API;
the tag and metadata lane do not perform these):

1. In App Store Connect, create the new version on the app page ("+" version)
   if it doesn't exist.
2. Require successful Test and Archive actions, then attach the processed build.
   A successful archive alone does not mean the workflow passed.
3. Assign the build to the intended TestFlight beta group and verify
   `buildBetaDetail.internalBuildState == IN_BETA_TESTING`.
4. Upload current release notes and update App Review instructions for this
   version. Upload changed screenshots for both iPhone 6.9-inch and iPad
   13-inch classes; language fallback does not replace device-class coverage.
5. Submit for Review and verify `WAITING_FOR_REVIEW` (or a later review state).
   The ASC API supports `reviewSubmissions` plus `reviewSubmissionItems`;
   create the submission, add the app version, then set `submitted: true`.
   Preserve the chosen release policy. `AFTER_APPROVAL` releases publicly
   after Apple approves; review submission alone is not public availability.
6. Keep Apple-silicon Mac distribution opted out: the SMS handoff is not
   supported there.

### iOS store metadata — fastlane

App Store metadata (release notes, descriptions, URLs, keywords) lives in
`fastlane/metadata/<locale>/` (20 locales) and is pushed with:

```bash
# from the repo root
fastlane ios store_metadata
```

- Metadata-only: `fastlane/Deliverfile` sets `skip_binary_upload`,
  `skip_screenshots`, `submit_for_review(false)`. Builds stay Xcode Cloud's
  job.
- Needs three env vars: `APP_STORE_CONNECT_API_KEY_ID`,
  `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_CONTENT`
  (base64 by default; `APP_STORE_CONNECT_API_KEY_IS_KEY_CONTENT_BASE64=false`
  to override). The API key is held personally by Vincent — it is not in the
  repo, CI, or shell profiles, and stays that way.
- **Gotcha:** the Deliverfile deliberately pins no `app_version`, so deliver
  needs an *editable* ASC version to exist. If it loops on
  `Cannot find edit app store version… retrying`, create the new version in
  ASC first (manual step 1 above), then re-run.
- Validate before pushing: `python3 scripts/validate-app-store-metadata.py`
  checks all 20 locales against ASC field limits (name 30, subtitle 30,
  promo 170, keywords 100, release notes 4000, …).

---

## Version bump

One PR, both platforms, so the version numbers stay aligned:

- `android/app/build.gradle.kts` — set the plain `versionName` literal and
  increment the plain `versionCode` literal. The tag does not override them;
  CI verifies the tag against the checked-in version before building.
- `ios/project.yml` — `MARKETING_VERSION`, then `cd ios && xcodegen generate`
  and commit the regenerated pbxproj.

pbxproj landmines: stage it with `git add -f` (an unanchored `SIT.xcodeproj`
gitignore rule silently no-ops a plain `git add`), and strip any non-empty
`DEVELOPMENT_TEAM = <ID>` hunks before committing (empty `""` is fine).

## Tagging

Tag the release commit on `main` after the notes + bump PRs merge:

```bash
git tag android/v<x.y.z>-production <commit>
git tag ios/v<x.y.z> <commit>
git push origin android/v<x.y.z>-production ios/v<x.y.z>
```

Pushing the Android tag **is** the production deploy — make sure notes and
version bump are already on the tagged commit. Tag conventions history:
`~/Documents/SecondBrain/Projects/Ticklr/Release Tagging Guide.md`.

## Post-release checklist

- Watch the **Android Release** run in GitHub Actions to green.
- Watch Xcode Cloud Test and Archive to success; attach the processed build,
  enable TestFlight, and submit the App Store version for review.
- Run `fastlane ios store_metadata` if metadata changed (after the editable ASC
  version exists and before review submission). Read back all 20 locale notes
  from ASC to verify. For a notes-only correction, stage only release-note
  files, pin `app_version`, and set `skip_app_version_update: true` to avoid
  replacing unrelated live metadata or changing the version.
- Publish changed Play listing text/graphics separately with
  `publishReleaseListing`; skip it when the listing is unchanged.
- Regenerate `fastlane/metadata/android/` after source metadata changes; compare
  the current version-code changelogs as well as validating the sources. Keep
  historical changelogs when committing the regenerated tree.
- Verify the GitHub release APK download, SHA-256, and signing certificate
  against `AllowedAPKSigningKeys` in `docs/fdroid/com.xaymaca.sit.yml`.
- Check the actual F-Droid submission state. Its initial MR must be merged
  before automatic tag polling can distribute releases; see
  [F-Droid status and follow-up](fdroid/README.md).
- Update `CHANGELOG.md` and Linear with the released version and any external
  review still pending. Do not mark public availability until verified.
- Verify the store pages once each platform goes live.
