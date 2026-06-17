#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import sys


METADATA_DIR = Path("fastlane/metadata")

EXPECTED_LOCALES = {
    "ar-SA",
    "cs",
    "de-DE",
    "el",
    "en-US",
    "es-ES",
    "fr-FR",
    "he",
    "hi",
    "hu",
    "it",
    "ja",
    "ko",
    "nl-NL",
    "pl",
    "pt-PT",
    "ro",
    "ru",
    "sv",
    "zh-Hans",
}

FIELD_LIMITS = {
    "name.txt": 30,
    "subtitle.txt": 30,
    "promotional_text.txt": 170,
    "description.txt": 4000,
    "keywords.txt": 100,
    "release_notes.txt": 4000,
    "privacy_url.txt": 255,
    "support_url.txt": 255,
    "marketing_url.txt": 255,
}


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    if not METADATA_DIR.is_dir():
        fail(f"Missing {METADATA_DIR}")

    actual_locales = {p.name for p in METADATA_DIR.iterdir() if p.is_dir()}
    missing = sorted(EXPECTED_LOCALES - actual_locales)
    extra = sorted(actual_locales - EXPECTED_LOCALES)
    errors: list[str] = []

    if missing:
        errors.append("Missing locale folders: " + ", ".join(missing))
    if extra:
        errors.append("Unexpected locale folders: " + ", ".join(extra))

    for locale in sorted(EXPECTED_LOCALES & actual_locales):
        folder = METADATA_DIR / locale
        for filename, limit in FIELD_LIMITS.items():
            path = folder / filename
            if not path.is_file():
                errors.append(f"{locale}: missing {filename}")
                continue
            text = path.read_text(encoding="utf-8").strip()
            if not text:
                errors.append(f"{locale}: {filename} is empty")
                continue
            count = len(text)
            if count > limit:
                errors.append(f"{locale}: {filename} is {count} chars; limit is {limit}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)

    print(f"OK: App Store metadata valid for {len(EXPECTED_LOCALES)} locales")


if __name__ == "__main__":
    main()
