# Tutorial localization

The 21 language JSON files are the shared source for tutorial text. Run
`python3 scripts/sync-tutorial-localizations.py` after editing them; use `--check`
to check generated iOS and Android resources without writing. Named placeholders
resolve against each platform's existing translated button labels, so the guide
names the control that actually appears in that app. Preserve placeholders and
proper names such as Alice Nguyen and Connections.csv.

iOS examples are native SwiftUI views and use the translated tutorial catalog.
Android examples are original native app captures, selected by Android's language
resource qualifiers. Hebrew has both modern `he` and legacy `iw` aliases.

To refresh Android images, build/install the debug and androidTest APKs on the
isolated `TicklrTutorials` emulator, set font scale to 1.0, then run
`python3 scripts/capture-tutorial-screenshots.py`. Optional `--languages` limits
recapture to specific JSON language codes. The script refuses physical devices
and other AVDs. Captures use bundled fake contacts, light mode, a 1080×2340
API 37 emulator and actual localized screens. They do not import personal contacts,
grant contact access, save reminders/groups, or replace screenshot text with overlays.
The contacts permission banner remains visible because permission is not granted.

Run `python3 scripts/validate-tutorial-screenshots.py` to verify all 168 images,
their capture hashes, dimensions and Hebrew aliases. `screenshots.json` records
capture provenance by content hash. Run the opt-in `TutorialLocalizedGuidesTest`
instrumentation class with `validateTutorialLocalization=true` and `locale=<tag>`
for native large-text guide checks; this does not overwrite the shipped images.
