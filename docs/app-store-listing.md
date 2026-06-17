# Ticklr — App Store Listing Copy

Repo-managed App Store Connect metadata lives under `fastlane/metadata/`.
Use `python3 scripts/validate-app-store-metadata.py` before uploading, then
`bundle exec fastlane ios store_metadata` to upload metadata only. Fastlane is
not used for builds, signing, TestFlight, screenshots, or binary uploads.

## App Name
Ticklr

## Subtitle *(30 chars max)*
Your personal network manager

## Promotional Text *(170 chars — editable any time without re-review)*
A warmer Ticklr: a fresh icon, hand-crafted artwork in every language, and smoother texting — send a note and land right back where you were.

> Evergreen fallback (reusable between releases): *No account. No cloud. No algorithm deciding who matters. Just you, your people, and a gentle nudge when it's time to reach out.*

## Description *(4000 chars max — first ~3 lines visible before "more")*

Your people matter. Stay in touch on your terms.

Ticklr is a privacy-first contact network manager that helps you maintain meaningful relationships — without algorithms, social feeds, or a cloud account. Everything lives on your device.

Set recurring reminders to reach out. Call your dad every Sunday. Check in with a colleague every quarter. Ticklr quietly notifies you when it's time, then schedules the next reminder automatically.

FEATURES

— Tickle calendar: set daily, weekly, monthly, or custom recurring reminders per contact
— Import contacts from your iPhone or LinkedIn CSV export
— Organize into groups: Family, Work, Board, Friends — any label you want
— Compose SMS and MMS to multiple contacts at once using saved message templates
— 100% on-device: no account, no cloud sync, no analytics, no tracking

YOUR DATA STAYS ON YOUR PHONE

Ticklr has no servers and collects no data. Your contacts, reminders, and messages are stored exclusively in your device's secure local database. We have no way to see your data — because we never receive it.

## Keywords *(100 chars max, comma-separated — no spaces after commas)*
crm,contacts,relationship,reminder,keep in touch,follow up,birthday,friends,family,address book

> 95 chars. Words already indexed from name/subtitle (personal, network, manager, Ticklr) are
> deliberately omitted; Apple combines fields, so "personal crm" etc. still match. Singulars only —
> plural matching is automatic. Keywords only update with a version submission, unlike promo text.

## Category
- Primary: Productivity
- Secondary: Utilities

## URLs
- Support URL: https://xaymaca.com/sit/support
- Marketing URL: https://xaymaca.com/sit
- Privacy Policy URL: https://xaymaca.com/sit/privacy

## App Store Screenshot Notes *(iPhone 6.9" required — up to 10)*

Recommended screenshot order:
1. Splash / launch screen (Ticklr wordmark + EKG animation)
2. Network list — contacts imported, scrolled to show variety
3. Contact detail — name, company, tickle badge
4. Tickle list — Due section with 2-3 due contacts visible
5. Tickle edit — frequency picker open
6. Groups list — several groups visible
7. Compose — multi-select with 3+ contacts chosen
8. Settings — clean overview

Tips:
- Use Settings > Debug > Clear All Contacts then Load Test Contacts for clean 20-contact dataset
- Status bar should show 9:41, full signal, 100% battery — use iOS Simulator or Demo Mode
- iPhone 17 Pro Max screenshots (6.9") are accepted for the primary size class

## App Store Screenshot Notes *(13" iPad Pro required for universal builds — up to 10)*

Universal binary (TIC-43): once `TARGETED_DEVICE_FAMILY = "1,2"`, App Store Connect requires 13" iPad Pro screenshots (2064×2752 portrait or 2752×2064 landscape).

Recommended iPad screenshot set — capture in landscape to showcase the two-pane split view that's unique to the iPad build:
1. Network — sidebar (contacts list) + detail pane (open contact)
2. Groups — sidebar (5 canonical groups) + detail pane (open group's members)
3. Tickle — list view with Due section + Milestones hero card visible
4. Compose — recipient picker + template + draft body
5. Settings — full settings panel

Capture commands (iPad Pro 13-inch M5 simulator):
```
# Boot and seed
xcrun simctl boot "iPad Pro 13-inch (M5)"
# In Settings > Debug, run Load Test Contacts
xcrun simctl io booted screenshot ~/Desktop/ipad-01-network.png
```

Tips:
- Landscape is the recommended orientation — it shows the split view's value (sidebar + detail).
- Portrait is also accepted; the sidebar collapses into the detail pane automatically.

## Version Notes (first submission)
- Version: 1.0
- What's New: Initial release
