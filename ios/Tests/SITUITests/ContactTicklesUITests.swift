import XCTest

final class ContactTicklesUITests: XCTestCase {
    private func reveal(_ row: XCUIElement, in app: XCUIApplication) {
        let profile = app.scrollViews.firstMatch
        XCTAssertTrue(profile.waitForExistence(timeout: 5))
        // Saving can restore the previous scroll position above OR below the
        // row. Small gestures in the profile avoid overshooting it on phones.
        for _ in 0..<10 where !row.isHittable {
            let rowFrame = row.frame
            let viewport = profile.frame
            let above = rowFrame != .zero && rowFrame.midY < viewport.midY
            let start = profile.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: above ? 0.35 : 0.75))
            let end = profile.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: above ? 0.75 : 0.35))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(row.isHittable, "Saved tickle should be readable by scrolling the profile")
    }

    func testCreateEditAndCancelReturnToSameProfile() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments += ["-hasCompletedOnboarding", "YES", "-debugSeedDemoData", "YES", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        app.openTab("network")
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
        note.typeText(marker + "\n")
        let save = app.buttons["tickleEdit.save"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        XCTAssertTrue(create.waitForExistence(timeout: 5))
        // The entire row is a button whose label includes note, frequency and date.
        let saved = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", marker)).firstMatch
        XCTAssertTrue(saved.waitForExistence(timeout: 5), "Saved tickle must appear immediately on the profile")
        reveal(saved, in: app)
        let rowID = saved.identifier
        saved.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5), "Tapping a profile tickle should open its editor")
        XCTAssertEqual(note.value as? String, marker, "The editor must load the tapped tickle")
        let edited = "Edited tickle \(UUID().uuidString.prefix(8))"
        // Tap beyond the short note's text so deletion starts at its end.
        note.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        note.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: marker.count))
        note.typeText(edited + "\n")
        save.tap()
        XCTAssertTrue(create.waitForExistence(timeout: 5), "Saving should return to the same profile")
        let editedRow = app.buttons[rowID]
        XCTAssertTrue(editedRow.waitForExistence(timeout: 5))
        XCTAssertTrue(editedRow.label.contains(edited), "Saving edits must update the same tickle row")
        XCTAssertTrue(person.exists, "The originating person's profile must remain visible")

        reveal(editedRow, in: app)
        editedRow.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        XCTAssertEqual(note.value as? String, edited)
        note.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        note.typeText(" unsaved\n")
        app.buttons["tickleEdit.cancel"].tap()
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
