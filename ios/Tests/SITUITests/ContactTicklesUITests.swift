import XCTest

final class ContactTicklesUITests: XCTestCase {
    func testCreateEditAndCancelReturnToSameProfile() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments += ["-hasCompletedOnboarding", "YES", "-debugSeedDemoData", "YES", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        let networkTab = app.buttons["Network"].firstMatch
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
        // The entire row is a button whose label includes note, frequency and date.
        let saved = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", marker)).firstMatch
        XCTAssertTrue(saved.waitForExistence(timeout: 5), "Saved tickle must appear immediately on the profile")
        for _ in 0..<4 where !saved.isHittable { app.swipeUp() }
        XCTAssertTrue(saved.isHittable, "Saved tickle should be readable by scrolling the profile")
        let rowID = saved.identifier
        saved.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5), "Tapping a profile tickle should open its editor")
        XCTAssertEqual(note.value as? String, marker, "The editor must load the tapped tickle")
        let edited = "Edited tickle \(UUID().uuidString.prefix(8))"
        // Tap beyond the short note's text so deletion starts at its end.
        note.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        note.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: marker.count))
        note.typeText(edited)
        app.buttons["Save"].tap()
        XCTAssertTrue(create.waitForExistence(timeout: 5), "Saving should return to the same profile")
        let editedRow = app.buttons[rowID]
        XCTAssertTrue(editedRow.waitForExistence(timeout: 5))
        XCTAssertTrue(editedRow.label.contains(edited), "Saving edits must update the same tickle row")
        XCTAssertTrue(person.exists, "The originating person's profile must remain visible")

        for _ in 0..<4 where !editedRow.isHittable { app.swipeUp() }
        editedRow.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        XCTAssertEqual(note.value as? String, edited)
        note.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        note.typeText(" unsaved")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(editedRow.waitForExistence(timeout: 5), "Cancelling should return to the same profile")
        XCTAssertTrue(editedRow.label.contains(edited))
        XCTAssertFalse(editedRow.label.contains("unsaved"), "Cancel must discard draft changes")
        XCTAssertTrue(create.exists)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Profile with assigned tickles"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
