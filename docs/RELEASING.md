# Releasing Ticklr

How a release gets from `main` to the App Store and Google Play. Both pipelines
are **tag-driven**: pushing a tag is the deploy button. Everything before the
tag is ordinary PR work; everything after is automated (Android) or
semi-automated (iOS).

Most recent walkthrough of this whole flow: v1.12.0, tagged on commit `7f49f6c`
(July 2026).

## The flow at a glance

```
1. Land all release work on main (PRs, never stacked)
2. Write + localize release notes  ──►  PR
3. Version bump                    ──►  PR
4. Tag the release commit:
     android/v<x.y.z>-production   ──►  GitHub Actions builds AAB → publishes to Play
     ios/v<x.y.z>                  ──►  Xcode Cloud builds → TestFlight/ASC
5. iOS manual steps in App Store Connect:
     create the version, attach build, Submit for Review
6. iOS store metadata (if changed): fastlane ios store_metadata
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
2. Computes `versionCode = 100 + GITHUB_RUN_NUMBER` (monotonic; do not set it
   manually).
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

**Scope caveat:** `publishReleaseBundle` ships the bundle + release notes only.
The store **listing** (title, descriptions, screenshots, contact/privacy URLs)
is not touched — listing changes are made in the Play Console by hand.

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

**Manual steps after the build** (nothing in the repo does these):

1. In App Store Connect, create the new version on the app page ("+" version)
   if it doesn't exist.
2. Attach the Xcode Cloud build to it.
3. Submit for Review.

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

- `android/app/build.gradle.kts` — the `versionName` fallback default
  (the tag overrides it in CI, but keep it honest).
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
- Watch Xcode Cloud in ASC; then version → attach build → Submit for Review.
- Run `fastlane ios store_metadata` if metadata changed (after the ASC version
  exists).
- Play Console listing changes (URLs, descriptions, screenshots) — by hand in
  the console; the pipeline never touches them.
- Verify the store pages once each platform goes live.
