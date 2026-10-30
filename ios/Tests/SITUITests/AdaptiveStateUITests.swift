import XCTest

/// TIC-111: deterministic compact/regular environment transitions on the same
/// live view tree, including while a modal is up. Physical Duo folding remains
/// a separate device check; orientation requests alone cannot prove resizing.
final class AdaptiveStateUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-debugTestAdaptiveResize", "-debugSeedDemoData", "YES",
                                "-hasCompletedOnboarding", "YES", "-tickleNotificationsEnabled", "NO",
                                "-debugForceCanSendMessages", "YES",
                                "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.buttons["test.resize"].firstMatch.waitForExistence(timeout: 15))
        return app
    }

    private func resize(_ app: XCUIApplication, to size: String) {
        let controls = app.buttons.matching(identifier: "test.resize")
        XCTAssertTrue(controls.firstMatch.waitForExistence(timeout: 5))
        let control = controls.allElementsBoundByIndex.first(where: { $0.isHittable }) ?? controls.firstMatch
        control.tap()
        // Re-resolve after adapting: the tappable control can move between
        // the root and a modal, so a bound index may no longer identify it.
        let updated = controls.firstMatch
        XCTAssertTrue(updated.waitForExistence(timeout: 5))
        XCTAssertEqual(updated.value as? String, size)
    }

    private func cycle(_ app: XCUIApplication, verify: () -> Void) {
        for _ in 0..<2 {
            resize(app, to: "regular")
            verify()
            resize(app, to: "compact")
            verify()
        }
    }

    private func openSearch(in app: XCUIApplication) -> XCUIElement {
        let search = app.searchFields.firstMatch
        if !search.exists {
            if app.buttons["Search"].firstMatch.exists {
                app.buttons["Search"].firstMatch.tap()
            } else {
                app.swipeDown()
            }
        }
        XCTAssertTrue(search.waitForExistence(timeout: 5))
        return search
    }

    func testContactEditDraftSurvivesResizeAndCancel() {
        let app = launch()
        app.openTab("network")
        app.staticTexts["Fatima Al-Hassan"].firstMatch.tap()
        app.buttons["Edit"].firstMatch.tap()
        let name = app.textFields["contactEdit.firstName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap()
        name.typeText(" draft")
        cycle(app) { XCTAssertEqual(name.value as? String, "Fatima draft") }
        app.buttons["Cancel"].firstMatch.tap()
        XCTAssertFalse(name.exists)
        XCTAssertTrue(app.buttons["Send a text"].exists)
    }

    func testNewContactDraftSurvivesResize() {
        let app = launch()
        app.openTab("network")
        app.descendants(matching: .any).matching(identifier: "network.add").firstMatch.tap()
        app.buttons["New Contact"].firstMatch.tap()
        let name = app.textFields["contactEdit.firstName"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap()
        name.typeText("Unsaved person")
        cycle(app) { XCTAssertEqual(name.value as? String, "Unsaved person") }
        app.buttons["Cancel"].firstMatch.tap()
        XCTAssertFalse(name.exists)
    }

    func testComposeRecipientTemplateAndDraftSurviveResize() {
        let app = launch()
        app.openTab("network")
        app.staticTexts["Fatima Al-Hassan"].firstMatch.tap()
        app.buttons["Send a text"].tap()
        XCTAssertTrue(app.buttons["compose.cancel"].waitForExistence(timeout: 5))
        let template = app.buttons["compose.template"].firstMatch
        for _ in 0..<3 {
            if template.exists { break }
            app.swipeUp()
        }
        if !template.waitForExistence(timeout: 5) {
            let hierarchy = XCTAttachment(string: app.debugDescription)
            hierarchy.lifetime = .keepAlways
            add(hierarchy)
            XCTFail("Compose template menu should be reachable")
        }
        template.tap()
        app.buttons["Checking in"].tap()
        let editor = app.textViews.firstMatch
        let original = editor.value as? String ?? ""
        XCTAssertFalse(original.isEmpty)
        editor.tap()
        editor.typeText(" Extra draft")
        let draft = editor.value as? String
        XCTAssertNotEqual(draft, original)
        XCTAssertTrue(draft?.contains("Extra draft") == true)
        cycle(app) {
            XCTAssertEqual(editor.value as? String, draft)
            XCTAssertTrue(app.staticTexts["Fatima Al-Hassan"].firstMatch.exists)
            XCTAssertTrue(app.buttons["compose.template"].firstMatch.label.contains("Checking in"))
        }
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<10 {
            let frame = element.exists ? element.frame : .zero
            if element.exists && element.isHittable && frame.midY < app.frame.height * 0.65
                && frame.midY > app.frame.height * 0.25 { return }
            let above = frame != .zero && frame.midY < app.frame.height * 0.25
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: above ? 0.4 : 0.65))
            let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: above ? 0.65 : 0.4))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(element.isHittable)
    }

    private func showSidebarIfNeeded(in app: XCUIApplication) {
        // At a narrow physical frame, the regular split may use an overlay
        // sidebar. Reveal it before checking retained list/search state.
        let show = app.buttons["Show Sidebar"].firstMatch
        if show.exists && show.isHittable { show.tap() }
    }

    func testNetworkSearchAndContactSelectionSurviveResize() {
        let app = launch()
        app.openTab("network")
        let search = openSearch(in: app)
        search.tap()
        search.typeText("Fatima")
        cycle(app) {
            if !search.exists { showSidebarIfNeeded(in: app) }
            XCTAssertEqual(openSearch(in: app).value as? String, "Fatima")
        }
        let contact = app.staticTexts["Fatima Al-Hassan"].firstMatch
        XCTAssertTrue(contact.waitForExistence(timeout: 5))
        contact.tap()
        let send = app.buttons["Send a text"]
        XCTAssertTrue(send.waitForExistence(timeout: 5))
        cycle(app) { XCTAssertTrue(send.exists, "Selected contact must remain open") }
    }

    func testTickleScrollContextSurvivesResize() {
        let app = launch()
        let row = app.cells.containing(.staticText, identifier: "Quick hello").firstMatch
        reveal(row, in: app)
        cycle(app) {
            if !row.exists || !row.isHittable { showSidebarIfNeeded(in: app) }
            XCTAssertTrue(row.isHittable, "Resize must retain the visible list context")
        }
    }

    func testTickleEditSavesOnceAfterResize() {
        let app = launch()
        let row = app.cells.containing(.staticText, identifier: "Quick hello").firstMatch
        reveal(row, in: app)
        row.swipeLeft()
        app.buttons["Edit"].firstMatch.tap()
        let note = app.textFields["tickleEdit.note"]
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        note.tap()
        note.typeText(" after resize\n")
        cycle(app) { XCTAssertEqual(note.value as? String, "Quick hello after resize") }
        app.buttons["tickleEdit.save"].tap()
        XCTAssertFalse(note.exists)
        let savedRows = app.cells.containing(.staticText, identifier: "Quick hello after resize")
        XCTAssertTrue(savedRows.firstMatch.waitForExistence(timeout: 5))
        XCTAssertEqual(savedRows.count, 1, "A resized editor must save one reminder")
        // Restore the fixture so idempotent demo-data launches can locate it.
        savedRows.firstMatch.swipeLeft()
        app.buttons["Edit"].firstMatch.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        note.tap()
        note.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: " after resize".count) + "\n")
        app.buttons["tickleEdit.save"].tap()
    }

    func testGroupSelectionAndSearchSurviveResize() {
        let app = launch()
        app.openTab("groups")
        app.buttons["groupRow.canonical.family"].tap()
        let addMembers = app.buttons["groupDetail.addMembers"]
        XCTAssertTrue(addMembers.waitForExistence(timeout: 5))
        let search = openSearch(in: app)
        search.tap()
        search.typeText("Alice")
        cycle(app) {
            XCTAssertTrue(addMembers.exists, "Selected group must remain open")
            XCTAssertEqual(search.value as? String, "Alice", "Member search must survive resize")
        }
    }

    func testGroupCreationDraftSurvivesResize() {
        let app = launch()
        app.openTab("groups")
        app.buttons["plus"].firstMatch.tap()
        let name = app.textFields["groupEdit.name"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap()
        name.typeText("Resize circle\n")
        cycle(app) { XCTAssertEqual(name.value as? String, "Resize circle") }
        let cancel = app.buttons["groupEdit.cancel"]
        if !cancel.exists {
            let before = XCTAttachment(screenshot: app.screenshot())
            before.name = "Group toolbar before revealing navigation"
            before.lifetime = .keepAlways
            add(before)
            app.collectionViews.firstMatch.swipeDown()
        }
        if !cancel.waitForExistence(timeout: 10) {
            let screenshot = XCTAttachment(screenshot: app.screenshot())
            screenshot.name = "Group sheet after layout transitions"
            screenshot.lifetime = .keepAlways
            add(screenshot)
            XCTFail("Group cancellation action must remain reachable")
        }
        cancel.tap()
        XCTAssertFalse(name.exists)
        XCTAssertTrue(app.buttons["groupRow.canonical.family"].exists)
    }

    func testCompactTickleDraftSurvivesResize() {
        let app = launch()
        app.buttons["plus"].firstMatch.tap()
        let note = app.textFields["tickleEdit.note"]
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        note.tap()
        note.typeText("Keep my tickle draft\n")
        cycle(app) { XCTAssertEqual(note.value as? String, "Keep my tickle draft") }
        app.buttons["tickleEdit.cancel"].tap()
        XCTAssertFalse(note.exists)
    }

    func testWideTickleDraftSurvivesCollapseAndExpansion() {
        let app = launch()
        resize(app, to: "regular")
        app.buttons["plus"].firstMatch.tap()
        let note = app.textFields["tickleEdit.note"]
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        note.tap()
        note.typeText("Keep my pane draft\n")
        for _ in 0..<2 {
            resize(app, to: "compact")
            XCTAssertEqual(note.value as? String, "Keep my pane draft")
            resize(app, to: "regular")
            XCTAssertEqual(note.value as? String, "Keep my pane draft")
        }
        resize(app, to: "compact")
        let back = app.buttons["BackButton"]
        XCTAssertTrue(back.waitForExistence(timeout: 5))
        back.tap()
        app.buttons["plus"].firstMatch.tap()
        XCTAssertTrue(note.waitForExistence(timeout: 5))
        XCTAssertEqual(note.value as? String, "Keep my pane draft", "Add must resume the hidden draft")
        app.buttons["tickleEdit.cancel"].tap()
        XCTAssertFalse(note.exists)
    }

    func testComposeDraftAndSelectedTabSurviveResize() {
        let app = launch()
        app.openTab("compose")
        let editor = app.textViews.firstMatch
        XCTAssertTrue(editor.waitForExistence(timeout: 5))
        editor.tap()
        editor.typeText("A message I have not sent")
        cycle(app) {
            XCTAssertEqual(editor.value as? String, "A message I have not sent")
            let symbol = app.buttons["square.and.pencil"].firstMatch
            let tab = symbol.exists ? symbol : app.buttons["Compose"].firstMatch
            XCTAssertTrue(tab.isSelected, "Compose tab must remain selected")
        }
    }
}
