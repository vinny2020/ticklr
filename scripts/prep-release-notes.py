#!/usr/bin/env python3
"""
prep-release-notes.py — translate Play Store release notes from English into
every locale Ticklr ships in.

Source of truth: android/app/src/main/play/release-notes/en-US/<track>.txt
Targets:         android/app/src/main/play/release-notes/<locale>/<track>.txt

Run BEFORE tagging a release. Commit the result, then tag and push as usual.

Usage:
    python3 scripts/prep-release-notes.py production
    python3 scripts/prep-release-notes.py internal --dry-run
    python3 scripts/prep-release-notes.py beta --only ja-JP,de-DE
    OPENAI_MODEL=gpt-5.5 python3 scripts/prep-release-notes.py production

The OpenAI API key must be set in $OPENAI_API_KEY.
"""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# Locale map: Play Console listing tag -> human-readable language name
#
# These cover the 21 app locales declared in resourceConfigurations.
# Notes:
#   - These match the Play listing directories that the API accepted.
#   - Arabic, Romanian, and Urdu are unqualified (no region).
#   - Portuguese is pt-PT to match Ticklr's existing Play listing locale.
#   - Chinese is zh-CN (Simplified) — add zh-TW separately if Taiwan matters.
#   - Spanish is es-ES; revisit if Latin America install share grows.
# ---------------------------------------------------------------------------
LOCALES: dict[str, str] = {
    "es-ES": "Spanish (Spain)",
    "fr-FR": "French",
    "de-DE": "German",
    "it-IT": "Italian",
    "nl-NL": "Dutch",
    "el-GR": "Greek",
    "pl-PL": "Polish",
    "ro": "Romanian",
    "hu-HU": "Hungarian",
    "pt-PT": "European Portuguese",
    "sv-SE": "Swedish",
    "cs-CZ": "Czech",
    "ru-RU": "Russian",
    "zh-CN": "Simplified Chinese",
    "ja-JP": "Japanese",
    "ko-KR": "Korean",
    "hi-IN": "Hindi",
    "ar": "Arabic",
    "he-IL": "Hebrew",
    "ur": "Urdu",
}

PLAY_NOTES_DIR = Path("android/app/src/main/play/release-notes")
PLAY_RELEASE_NOTES_LIMIT = 500  # Google Play hard cap per locale per track
DEFAULT_MODEL = os.environ.get("OPENAI_MODEL", "gpt-5.5")

TRANSLATE_PROMPT = """You are translating Google Play Store release notes for an \
Android app called Ticklr, a privacy-first contact reminder app.

Translate the following English release notes into {language}. Strict rules:

1. Output ONLY the translation. No preamble, no commentary, no quotes around it.
2. Keep the bullet-point structure EXACTLY as in the source (same number of \
bullets, same delimiters).
3. Keep brand names untranslated: "Ticklr", "Xaymaca".
4. Keep feature names that appear as proper nouns in the app untranslated \
unless there's a clear localization (e.g., "Add Tickle" → translate the \
verb "Add" but keep "Tickle" if it's a coined product term).
5. Keep the tone matter-of-fact and human, not corporate.
6. The result MUST be under 500 characters total. If a literal translation \
would exceed that, tighten it — drop articles, shorten phrasing, but never \
drop a whole bullet.
7. Use the locale's natural punctuation conventions (e.g., full-width punctuation \
for Chinese/Japanese, RTL-appropriate punctuation for Arabic/Hebrew/Urdu).

English source:
---
{english}
---

Translation:"""


def fail(msg: str, code: int = 1) -> None:
    print(f"✗ {msg}", file=sys.stderr)
    sys.exit(code)


def require_openai_sdk():
    try:
        from openai import OpenAI  # noqa: F401
        return OpenAI
    except ImportError:
        fail(
            "The `openai` Python package is not installed.\n"
            "  Install with:  pip3 install --user openai\n"
            "  Or in a venv:  python3 -m venv .venv && source .venv/bin/activate && pip install openai"
        )


def require_api_key():
    key = os.environ.get("OPENAI_API_KEY")
    if not key:
        fail(
            "OPENAI_API_KEY is not set.\n"
            "  export OPENAI_API_KEY=sk-...\n"
            "  (get one at https://platform.openai.com/api-keys)"
        )
    return key


def find_repo_root() -> Path:
    """Walk up until we find the directory containing 'android/app/src/main/play'."""
    cur = Path.cwd().resolve()
    for candidate in [cur, *cur.parents]:
        if (candidate / PLAY_NOTES_DIR).is_dir():
            return candidate
    fail(
        f"Could not find {PLAY_NOTES_DIR} from {cur}. "
        "Run this script from inside the ticklr repo."
    )


def translate(client, english: str, model: str, language: str) -> str:
    prompt = TRANSLATE_PROMPT.format(language=language, english=english)
    resp = client.responses.create(
        model=model,
        input=prompt,
        max_output_tokens=1024,
    )
    text = getattr(resp, "output_text", "").strip()
    if text:
        return text

    # Fallback for older SDK response objects that may not expose output_text.
    chunks: list[str] = []
    for item in getattr(resp, "output", []) or []:
        for content in getattr(item, "content", []) or []:
            if getattr(content, "type", "") == "output_text":
                chunks.append(getattr(content, "text", ""))
    return "".join(chunks).strip()


def main():
    parser = argparse.ArgumentParser(
        description="Translate Play Store release notes into all supported locales.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "track",
        nargs="?",
        default="production",
        choices=["internal", "alpha", "beta", "production"],
        help="Which release track's notes to translate (default: production).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print translations to stdout; do not write files.",
    )
    parser.add_argument(
        "--only",
        type=str,
        default=None,
        help="Comma-separated list of locale tags to translate (e.g., ja-JP,de-DE). "
        "Default: all 20 locales.",
    )
    parser.add_argument(
        "--model",
        type=str,
        default=DEFAULT_MODEL,
        help=f"OpenAI model to use (default: {DEFAULT_MODEL}). "
        "Can also be set with OPENAI_MODEL.",
    )
    args = parser.parse_args()

    OpenAI = require_openai_sdk()
    require_api_key()

    repo_root = find_repo_root()
    os.chdir(repo_root)
    print(f"  Repo root: {repo_root}")

    source_file = PLAY_NOTES_DIR / "en-US" / f"{args.track}.txt"
    if not source_file.is_file():
        fail(f"Source file not found: {source_file}")

    english = source_file.read_text(encoding="utf-8").strip()
    if not english:
        fail(f"Source file is empty: {source_file}")
    if len(english) > PLAY_RELEASE_NOTES_LIMIT:
        fail(
            f"⚠ Source is {len(english)} chars; Play caps at "
            f"{PLAY_RELEASE_NOTES_LIMIT}. Shorten en-US first."
        )

    print(f"  Track: {args.track}")
    print(f"  Source: {source_file} ({len(english)} chars)")
    print(f"  Model: {args.model}")
    print()
    print("English source:")
    print("─" * 60)
    print(english)
    print("─" * 60)
    print()

    targets = LOCALES
    if args.only:
        wanted = [t.strip() for t in args.only.split(",") if t.strip()]
        unknown = [t for t in wanted if t not in LOCALES]
        if unknown:
            fail(f"Unknown locale tag(s): {', '.join(unknown)}")
        targets = {k: LOCALES[k] for k in wanted}

    client = OpenAI()

    over_limit: list[tuple[str, int]] = []
    failed: list[tuple[str, str]] = []
    written: list[Path] = []

    for locale, language in targets.items():
        print(f"  → {locale} ({language})... ", end="", flush=True)
        try:
            translated = translate(client, english, args.model, language)
        except Exception as e:
            print(f"FAILED: {e}")
            failed.append((locale, str(e)))
            continue

        if not translated:
            print("FAILED: empty translation")
            failed.append((locale, "empty translation"))
            continue

        size = len(translated)
        flag = ""
        if size > PLAY_RELEASE_NOTES_LIMIT:
            flag = f"  ⚠ OVER LIMIT ({size} chars)"
            over_limit.append((locale, size))
        print(f"{size} chars{flag}")

        if args.dry_run:
            print("    " + translated.replace("\n", "\n    "))
            print()
        else:
            target_path = PLAY_NOTES_DIR / locale / f"{args.track}.txt"
            target_path.parent.mkdir(parents=True, exist_ok=True)
            target_path.write_text(translated + "\n", encoding="utf-8")
            written.append(target_path)

    print()
    if args.dry_run:
        print("Dry run complete — no files written.")
    else:
        print(f"✓ Wrote {len(written)} file(s).")
        print()
        print("Next steps:")
        print("  1. Review:   git diff android/app/src/main/play/release-notes/")
        print("  2. Commit:   git add android/app/src/main/play/release-notes/ "
              "&& git commit -m 'Release notes for v<x.y.z>'")
        print(f"  3. Tag:      git tag android/v<x.y.z>-{args.track}")
        print(f"  4. Push:     git push origin android/v<x.y.z>-{args.track}")

    if failed:
        print()
        print(f"✗ {len(failed)} locale(s) failed to translate:")
        for locale, error in failed:
            print(f"    {locale}: {error}")
        sys.exit(1)

    if over_limit:
        print()
        print(
            f"⚠ {len(over_limit)} locale(s) exceeded the 500-char limit. "
            "Play will reject these. Re-run --only <locale> after shortening "
            "the English source, or edit the offending files by hand:"
        )
        for locale, size in over_limit:
            print(f"    {locale}: {size} chars")
        sys.exit(2)


if __name__ == "__main__":
    main()
