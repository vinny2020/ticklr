#!/usr/bin/env python3
"""Validate complete, unchanged native tutorial captures for every app language."""
import hashlib
import importlib.util
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('capture', ROOT / 'scripts/capture-tutorial-screenshots.py')
capture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capture)
manifest = json.loads((ROOT / 'scripts/tutorial-localization/screenshots.json').read_text())
assert set(manifest) == set(capture.LANGUAGES), 'Incomplete screenshot languages'
res = ROOT / 'android/app/src/main/res'
for language, screens in manifest.items():
    assert set(screens) == set(capture.SCREENS), f'{language}: missing screen'
    qualifier = {'zh-Hans': 'zh'}.get(language, language)
    folder = 'drawable-nodpi' if language == 'en' else f'drawable-{qualifier}-nodpi'
    for screen, expected in screens.items():
        name = f'tutorial_android_{screen}.png'
        data = (res / folder / name).read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n', f'{language}/{screen}: not PNG'
        assert struct.unpack('>II', data[16:24]) == (1080, 2340), f'{language}/{screen}: unexpected dimensions'
        assert hashlib.sha256(data).hexdigest() == expected, f'{language}/{screen}: changed since capture'
        if language == 'he':
            assert data == (res / 'drawable-iw-nodpi' / name).read_bytes(), 'Hebrew alias differs'
        if language != 'en':
            assert expected != manifest['en'][screen], f'{language}/{screen}: English image copied'
print(f'{len(manifest)} languages × {len(capture.SCREENS)} native screenshots validated')
