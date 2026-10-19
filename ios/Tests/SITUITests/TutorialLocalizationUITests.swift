import XCTest

@MainActor
final class TutorialLocalizationUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testGermanGuidesAndExamples() { checkGuides(language: "de", settings: "Einstellungen", cancel: "Abbrechen") }
    func testJapaneseGuidesAndExamples() { checkGuides(language: "ja", settings: "設定", cancel: "キャンセル") }
    func testArabicGuidesWithAccessibilityText() { checkGuides(language: "ar", settings: "الإعدادات", cancel: "إلغاء", largeText: true) }
    func testHindiGuidesAndExamples() { checkGuides(language: "hi", settings: "सेटिंग", cancel: "रद्द करें") }
    func testUrduGuidesAndExamples() { checkGuides(language: "ur", settings: "ترتیبات", cancel: "منسوخ کریں") }

    private func checkGuides(language: String, settings: String, cancel: String, largeText: Bool = false) {
        let app = XCUIApplication()
        app.launchArguments = ["-hasCompletedOnboarding", "YES", "-AppleLanguages", "(\(language))", "-AppleLocale", language]
        if largeText { app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"] }
        app.launch()
        let settingsTab = app.tabBars.buttons[settings].firstMatch
        XCTAssertTrue(settingsTab.waitForExistence(timeout: 15))
        settingsTab.tap()
        let library = app.buttons["settings.tutorials"]
        for _ in 0..<6 where !library.isHittable {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4)))
        }
        XCTAssertTrue(library.isHittable)
        library.tap()
        for topic in ["phone", "linkedin", "tickle", "groups"] {
            let guide = app.buttons["tutorial.library.\(topic)"]
            XCTAssertTrue(guide.waitForExistence(timeout: 5))
            guide.tap()
            let next = app.buttons["tutorial.next"]
            XCTAssertTrue(next.waitForExistence(timeout: 5))
            XCTAssertTrue(next.isHittable, "\(language)/\(topic): navigation must remain reachable")
            XCTAssertNotEqual(next.label, "Next", "The guide must use the app language")
            attach(app, name: "\(language)-\(topic)-first")
            let progress = app.staticTexts["tutorial.progress"]
            let first = progress.label
            next.tap()
            XCTAssertNotEqual(progress.label, first)
            if topic == "tickle" { attach(app, name: "\(language)-birthday-example") }
            app.buttons["tutorial.back"].tap()
            XCTAssertEqual(progress.label, first)
            next.tap()
            next.tap()
            XCTAssertTrue(next.isHittable)
            app.buttons["tutorial.close"].tap()
            XCTAssertTrue(guide.waitForExistence(timeout: 5))
            // Replay still works after dismissing a translated guide.
            guide.tap()
            XCTAssertTrue(next.waitForExistence(timeout: 5))
            XCTAssertEqual(progress.label, first)
            app.buttons["tutorial.close"].tap()
        }
        // A localized final action opens the real editor without a sample contact.
        app.buttons["tutorial.library.tickle"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap(); next.tap(); next.tap()
        XCTAssertTrue(app.buttons[cancel].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["Alice Nguyen"].exists)
        app.buttons[cancel].tap()
        XCTAssertTrue(app.buttons["tutorial.library.tickle"].waitForExistence(timeout: 5))
    }

    private func attach(_ app: XCUIApplication, name: String) {
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = name
        capture.lifetime = .keepAlways
        add(capture)
    }
}
