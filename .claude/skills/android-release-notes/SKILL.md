---
name: android-release-notes
description: >-
  Write and localize Google Play release notes for a Ticklr Android release —
  rewrite the en-US note for what's actually shipping, translate it into all 21
  app locales, and validate each against Play's 500-character limit. Use when
  cutting an Android release, when asked to "update/prep/translate the release
  notes," or before pushing an android/v*-<track> tag. Android only.
---

# Android Release Notes (all locales)

Produce the per-locale Play Store release notes that ship with an Android release.
Files live at `android/app/src/main/play/release-notes/<play-tag>/<track>.txt`.
The CI workflow (`.github/workflows/android-release.yml`) uploads whatever is
checked in for the tagged track and **fails the build if any locale exceeds 500
characters**, so get this right before tagging.

## What this skill does NOT cover
- Cutting the tag / version bump / the actual deploy (that's the broader release step).
- The Play Store **listing** (title/short/full description). `publishReleaseBundle`
  ships the bundle + these release notes only — listings need a separate
  `publishReleaseListing`. Don't assume listing copy goes live with a release tag.

## Step 1 — Write the en-US source note

`release-notes/en-US/production.txt` is the source of truth; every other locale is
translated from it.

- **Match what's actually shipping.** Run `git log --oneline <last-tag>..HEAD`
  (last tag: `git tag -l 'android/v*' | sort -V | tail -1`) and write notes for
  those changes. The #1 failure mode here is shipping the *previous* release's
  notes — always re-read the current file and assume it's stale.
- Lead with the headline change; 3–5 bullets max, matter-of-fact, human tone.
- **Keep brand strings untranslated everywhere:** `Ticklr`, `Xaymaca`,
  `YOUR PEOPLE MATTER`. "Tickle" (the coined reminder term) usually stays too.
- **Keep it ≤ ~350 chars in English** to leave headroom — German/Russian/Greek
  expand and can blow past 500 if English is already long.
- Use `•` bullets (one per line). Preserve the exact bullet count across locales.

## Step 2 — Translate into all locales

Translate the en-US note into every locale below. **Prefer doing this natively**
(translate directly — no API key or dependency) and back-translate any RTL/CJK
locale to English to sanity-check meaning before writing. The locales (21 locales,
**one directory each**):

| Dir | Language | | Dir | Language |
|---|---|---|---|---|
| `es-ES` | Spanish (Spain) | | `cs-CZ` | Czech |
| `fr-FR` | French | | `ru-RU` | Russian |
| `de-DE` | German | | `zh-CN` | Simplified Chinese |
| `it-IT` | Italian | | `ja-JP` | Japanese |
| `nl-NL` | Dutch | | `ko-KR` | Korean |
| `el-GR` | Greek | | `hi-IN` | Hindi |
| `pl-PL` | Polish | | `ar` | Arabic (unqualified) |
| `ro` | Romanian (unqualified) | | `iw-IL` | Hebrew (legacy tag — **the only Hebrew dir**) |
| `hu-HU` | Hungarian | | `ur` | Urdu (unqualified) |
| `pt-PT` | European Portuguese | | `en-US` | English (the source) |
| `sv-SE` | Swedish | | | |

Per-locale rules: same bullet structure, brand names untranslated, locale-natural
punctuation (full-width for zh/ja, RTL-appropriate for ar/iw/ur), result < 500 chars.

> **Hebrew uses `iw-IL` ONLY — never also add `he-IL`.** Google's supported-languages
> table uses the legacy `iw-IL`, and the Play API normalizes both `he-IL` and `iw-IL`
> to the same Hebrew locale. Shipping both makes the edit **commit** fail with
> `400 — "Release notes are badly constructed or have duplicates."` (the per-file
> char check passes, so only the Play API catches it, at the very end of a release).
> We dropped `he-IL` after it broke the v1.9.0 production cut. See Linear TIC-42.

### Batch fallback
`scripts/prep-release-notes.py <track>` translates en-US → all locales via the
OpenAI API (needs `OPENAI_API_KEY` + `pip install openai`). Useful for bulk runs,
but the native approach above avoids the dependency and lets you verify quality
inline. `--dry-run` previews without writing; `--only ja-JP,de-DE` limits scope.

## Step 3 — Validate (same check CI runs)

```bash
cd android/app/src/main/play/release-notes
for f in */<track>.txt; do
  CHARS=$(LC_ALL=C.UTF-8 wc -m < "$f" | tr -d ' ')
  [ "$CHARS" -gt 500 ] && echo "OVER: $f ($CHARS)" || printf "%-10s %s\n" "$(dirname $f)" "$CHARS"
done
# Count chars, NOT bytes — non-Latin scripts are multi-byte; byte counts falsely fail.
grep -il "tablet\|<stale feature>" */<track>.txt   # sanity: nothing from the last release
ls */<track>.txt | wc -l                            # expect 21 for production (one Hebrew dir: iw-IL)
```

Every locale must be ≤ 500 chars and free of prior-release content. If a locale is
over, tighten that translation (drop articles/shorten) — never drop a bullet.

## Step 4 — Commit

Branch + PR (don't commit straight to `main`), staging **only** the release-notes
dir (+ any intended version bump). Keep unrelated working-tree changes (e.g. iOS)
out of the commit.

```bash
git add android/app/src/main/play/release-notes/
git commit -m "chore(android): <track> release notes for v<x.y.z> (NN locales)"
```

After merge, the broader release step tags `android/v<x.y.z>-<track>` on `main`.
