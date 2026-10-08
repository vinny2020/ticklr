import XCTest

final class ContactTicklesUITests: XCTestCase {
    func testCreateTickleAppearsOnProfileWithoutLeavingNetwork() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments += ["-hasCompletedOnboarding", "YES", "-debugSeedDemoData", "YES", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        let networkTab = app.tabBars.buttons.element(boundBy: 0)
        XCTAssertTrue(networkTab.waitForExistence(timeout: 15))
        networkTab.tap()
        let person = app.staticTexts["Fatima Al-Hassan"].firstMatch
        XCTAssertTrue(person.waitForExistence(timeout: 5))
        person.tap()
        let create = app.buttons["Create a tickle"]
        XCTAssertTrue(create.waitForExistence(timeout: 5))
        create.tap()
        let note = app.textFields["e.g. Ask about the new role"]
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        note.tap()
        let marker = "Profile tickle \(UUID().uuidString.prefix(8))"
        note.typeText(marker)
        app.buttons["Save"].tap()
        XCTAssertTrue(create.waitForExistence(timeout: 5))
        // Combined accessibility row includes note, frequency and date.
        let saved = app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", marker)).firstMatch
        XCTAssertTrue(saved.waitForExistence(timeout: 5), "Saved tickle must appear immediately on the profile")
        for _ in 0..<4 where !saved.isHittable { app.swipeUp() }
        XCTAssertTrue(saved.isHittable, "Saved tickle should be readable by scrolling the profile")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Profile with assigned tickles"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
