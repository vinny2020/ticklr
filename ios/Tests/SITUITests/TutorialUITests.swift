import XCTest

final class TutorialUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func openLibrary(extraArguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-hasCompletedOnboarding", "YES", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchArguments += extraArguments
        app.launch()
        let settings = app.tabBars.buttons.element(boundBy: 4)
        XCTAssertTrue(settings.waitForExistence(timeout: 15))
        settings.tap()
        let tutorials = app.buttons["settings.tutorials"]
        if !tutorials.exists { app.swipeUp() }
        XCTAssertTrue(tutorials.waitForExistence(timeout: 5))
        tutorials.tap()
        return app
    }

    func testAllGuidesCanBeDismissedAndReplayed() {
        let app = openLibrary()
        for topic in ["phone", "linkedin", "tickle", "groups"] {
            let guide = app.buttons["tutorial.library.\(topic)"]
            XCTAssertTrue(guide.waitForExistence(timeout: 5))
            guide.tap()
            let next = app.buttons["tutorial.next"]
            XCTAssertTrue(next.waitForExistence(timeout: 5))
            XCTAssertEqual(app.staticTexts["tutorial.progress"].label, "1 of 3")
            let attachment = XCTAttachment(screenshot: app.screenshot())
            attachment.name = "Tutorial-\(topic)-first-card"
            attachment.lifetime = .keepAlways
            add(attachment)
            next.tap()
            XCTAssertEqual(app.staticTexts["tutorial.progress"].label, "2 of 3")
            app.buttons["tutorial.back"].tap()
            XCTAssertEqual(app.staticTexts["tutorial.progress"].label, "1 of 3")
            app.buttons["tutorial.close"].tap()
            XCTAssertTrue(guide.waitForExistence(timeout: 5))
            // The library must ignore invitation dismissal history.
            guide.tap()
            XCTAssertTrue(next.waitForExistence(timeout: 5))
            XCTAssertEqual(app.staticTexts["tutorial.progress"].label, "1 of 3")
            app.buttons["tutorial.close"].tap()
        }
    }

    func testTickleGuideOpensRealEditorOnlyAfterFinalAction() {
        let app = openLibrary()
        app.buttons["tutorial.library.tickle"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        next.tap()
        XCTAssertEqual(app.staticTexts["tutorial.progress"].label, "3 of 3")
        XCTAssertEqual(next.label, "Create a tickle")
        next.tap()
        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 5))
        // Sample Alice must never be passed as a real editor target.
        XCTAssertFalse(app.staticTexts["Alice Nguyen"].exists)
        XCTAssertFalse(app.buttons["Save"].isEnabled)
        cancel.tap()
        XCTAssertTrue(app.buttons["tutorial.library.tickle"].waitForExistence(timeout: 5))
    }

    func testSwipeDismissalDoesNotOpenAnEditor() {
        let app = openLibrary()
        app.buttons["tutorial.library.tickle"].tap()
        XCTAssertTrue(app.buttons["tutorial.next"].waitForExistence(timeout: 5))
        app.navigationBars.firstMatch.swipeDown()
        XCTAssertTrue(app.buttons["tutorial.library.tickle"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Save"].exists)
    }
    func testGroupsGuideOpensGroupsAndCanReturnToLibrary() {
        let app = openLibrary()
        app.buttons["tutorial.library.groups"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        next.tap()
        next.tap()
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: 5))
        done.tap()
        XCTAssertTrue(app.buttons["tutorial.library.groups"].waitForExistence(timeout: 5))
    }

    func testLinkedInFinalActionOpensFilePicker() {
        let app = openLibrary()
        app.buttons["tutorial.library.linkedin"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        next.tap()
        next.tap()
        // The system Files picker is presented after the import screen appears.
        XCTAssertTrue(app.buttons["Cancel"].waitForExistence(timeout: 10))
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.buttons["Done"].waitForExistence(timeout: 5))
    }

    func testPhoneFinalActionOpensImportChoices() {
        let app = openLibrary()
        app.buttons["tutorial.library.phone"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        next.tap()
        next.tap()
        XCTAssertTrue(app.buttons["Choose contacts…"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Select iPhone Contacts"].exists)
        let done = app.buttons["Done"]
        XCTAssertTrue(done.exists)
        done.tap()
        XCTAssertTrue(app.buttons["tutorial.library.phone"].waitForExistence(timeout: 5))
    }

    func testAccessibilityTextKeepsNavigationAvailable() {
        let app = openLibrary(extraArguments: ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"])
        app.buttons["tutorial.library.tickle"].tap()
        let next = app.buttons["tutorial.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        XCTAssertTrue(next.isHittable)
        next.tap()
        XCTAssertTrue(next.isHittable)
        XCTAssertTrue(app.buttons["tutorial.close"].isHittable)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Tutorial-accessibility-text"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

}
