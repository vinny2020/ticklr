---
name: iOS build preferences
description: How to build and verify the iOS target
type: feedback
---

Don't strip code signing or use the simulator for iOS builds. Use `generic/platform=iOS` and let the signing team stay in place. The user has a real device and prefers building for it.

**Why:** User re-added their team after I stripped signing — they want real-device builds, not simulator workarounds.

**How to apply:** Always build with `xcodebuild -destination 'generic/platform=iOS'` and no CODE_SIGN_IDENTITY overrides. Filter out signing errors when checking for code errors since signing is expected to work with the team set.
