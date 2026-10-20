# iOS 1.15.1 tutorial store captures

Captured 19 October 2026 from the native app at `ac2a7c7` using
`TutorialUITests.testAllGuidesCanBeDismissedAndReplayed`.

| File | Device | Native dimensions | ASC device-class set |
|---|---|---|---|
| `iphone-69/tutorial-tickle.png` | iPhone 17 Pro Max, iOS 27.0 | 1320 × 2868 | APP_IPHONE_67 (accepted 6.9-inch dimensions) |
| `ipad-13/tutorial-tickle.png` | iPad Pro 13-inch M5, iPadOS 27.0 | 2064 × 2752 | APP_IPAD_PRO_3GEN_129 (13-inch) |

Unedited XCTest screenshots; fictional built-in example contact only. No device
contacts were imported. Both images were visually inspected and uploaded to the
existing en-US screenshot sets for App Store version 1.15.1, after the five
existing images. Both processed COMPLETE. English fallback is retained for
other store languages, independently for each device class.

Capture validation passed on both devices for all four tutorials, including
Next/Back, close, and replay. The test-only Settings selector was adjusted for
iPadOS's nested top-tab accessibility elements; shipping app code was unchanged.
