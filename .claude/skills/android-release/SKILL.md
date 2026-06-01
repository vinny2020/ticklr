---
name: android-release
description: >-
  Cut a Ticklr Android release end to end — decide the version and track,
  prep + localize release notes, bump the version, land it on main, then tag
  to trigger the CI build and Play Store publish. Use when asked to "cut/ship/
  release the Android app," "push a release," or tag an android/v* version.
  Covers the gotchas: notes-vs-listings, versionCode, Hebrew tags. Android only.
---

# Cut an Android Release

End-to-end Android release. The deploy is driven entirely by a git tag:
pushing `android/v<version>-<track>` to `main` triggers
`.github/workflows/android-release.yml`, which signs the AAB and runs
`publishReleaseBundle` to the chosen track.

> ⚠️ **Pushing a production tag deploys to all users and is irreversible.**
> Always confirm version + track + notes with the user before pushing the tag.
> Prefer a `beta` or `internal` tag first for large/visual changes.

## Step 1 — Pre-flight: what's shipping, and as what version?

```bash
git tag -l 'android/v*' | sort -V | tail -1          # last release tag
git log --oneline "$(git tag -l 'android/v*' | sort -V | tail -1)"..HEAD   # unreleased commits
```
- Read the unreleased commits and decide the **version** (semver: minor for
  features/redesign, patch for fixes) and **track** (`production` direct, or
  `beta`/`internal` first). These are the user's calls — confirm them.
- Current shipped `versionName` lives in `android/app/build.gradle.kts`
  (`defaultConfig.versionName`).

**Tag format & what CI derives:**
| Tag | Track | versionName | versionCode |
|---|---|---|---|
| `android/v1.9.0-production` | production | 1.9.0 | `100 + run#` (auto) |
| `android/v1.9.0-beta` | beta | 1.9.0 | auto |
| `android/v1.9.0-alpha` | alpha | 1.9.0 | auto |
| `android/v1.9.0` (no suffix) | internal | 1.9.0 | auto |

`versionCode` is **always** `100 + GITHUB_RUN_NUMBER` — never set by hand. CI
parses `versionName`/track from the tag and passes them to Gradle.

## Step 2 — Release notes

Localize the release notes for the chosen track → **invoke the
`android-release-notes` skill** (write en-US for what's shipping, translate to
all 21 locales, validate ≤500 chars). Don't ship the previous release's notes.

## Step 3 — Version bump

Bump `defaultConfig.versionName` in `android/app/build.gradle.kts` to the new
version (CI derives the shipped value from the tag, but keep the repo honest).

## Step 4 — Land prep on main

The tag must point at a `main` commit that already contains the notes + bump.
Branch → PR → merge (don't tag a branch). Stage only release-relevant files;
keep unrelated working-tree changes (e.g. iOS) out.

```bash
git switch -c chore/android-v<x.y.z>-release-prep
git add android/app/build.gradle.kts android/app/src/main/play/release-notes/
git commit -m "chore(android): prep v<x.y.z> release — notes + version bump"
# open PR, merge, then sync local main:
git switch main && git fetch origin && git merge --ff-only origin/main
```

## Step 5 — Tag & deploy (the irreversible step — confirm first)

After prep is merged and `main` is synced:
```bash
git tag -a android/v<x.y.z>-<track> -m "Android release <x.y.z> — <headline>"
git push origin android/v<x.y.z>-<track>
```
Watch the run: `gh run watch` (or the Actions tab). The job validates notes
(≤500 chars), decodes the keystore, and publishes the bundle.

## Step 6 — Post-deploy

- **Listings are NOT in this release.** `publishReleaseBundle` ships the bundle +
  release notes only. Localized store **listings** (title/short/full description,
  TIC-42) need a separate run:
  `./gradlew :app:publishReleaseListing` (validate-only first with
  `-PplayCommit=false` — see below). Run it when you intend to update listings.
- **Hebrew tags:** the listing publish carries both `he-IL` and `iw-IL`. After a
  real production publish, check Play Console's language list to see which renders
  and retire the redundant one (Linear TIC-42).
- **Update Linear:** move shipped issues to Done; note the released version.

### Validate-only Play publish (no go-live)
`-PplayCommit=false` uploads to a draft edit without committing — use it to
check listing metadata / locale tags against the Play API without touching
production. Listing publishes are **not** track-isolated; without this flag they
go straight to the live listing.

## Quick reference — gotchas
- Tag drives everything; `versionCode = 100 + run#`, never manual.
- 500-char release-note limit per locale (chars, not bytes) — CI fails the build otherwise.
- `publishReleaseBundle` = bundle + notes; **listings are separate**.
- Hebrew ships under both `he-IL` and `iw-IL` until confirmed.
- Never commit signing keys / `play-service-account.json` (CI injects via secrets).
