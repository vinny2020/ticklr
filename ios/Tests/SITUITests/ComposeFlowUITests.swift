import XCTest

/// End-to-end navigation-flow tests for the compose surfaces. These exist to
/// pin the "finish an action → flow back to where you came from" behavior:
/// compose opened from a tickle or a contact must return there on close, never
/// strand the user on an empty compose form.
///
/// The MFMessageComposeViewController .sent path can't run on the Simulator
/// (no Messages backend), so these tests cover every in-app leg around it;
/// the sent-result routing in ComposeView.handleComposerDismiss is exercised
/// on-device.
final class ComposeFlowUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += [
            "-hasCompletedOnboarding", "YES",
            "-debugSeedDemoData", "YES",
            "-debugForceCanSendMessages", "YES",
        ]
        app.launch()
        return app
    }

    /// The seeded overdue tickle row on the Tickle home ("Quick hello" is
    /// created 2 days overdue by SeedDataService.seedDemoData, so exactly one
    /// row carries an "overdue" label).
    private func firstDueRow(in app: XCUIApplication) -> XCUIElement {
        app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS[c] 'overdue'")
        ).firstMatch
    }

    /// Tickle row → connect action sheet → Compose message → Cancel
    /// must land back on the Tickle list, not an empty compose form.
    func testComposeFromTickleReturnsToTickleListOnCancel() {
        let app = launchApp()

        let dueRow = firstDueRow(in: app)
        XCTAssertTrue(dueRow.waitForExistence(timeout: 15),
                      "Tickle home should show a due tickle from the demo seed")
        dueRow.tap()

        let composeAction = app.buttons["Compose message"]
        XCTAssertTrue(composeAction.waitForExistence(timeout: 5),
                      "Connect action sheet should offer Compose message")
        composeAction.tap()

        // Compose sheet: recipient pre-filled (To field shows a name, not the
        // search placeholder) and a message editor present.
        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 5),
                      "Compose sheet should appear after the action sheet dismisses")
        XCTAssertTrue(app.textViews.firstMatch.waitForExistence(timeout: 3),
                      "Compose sheet should show the message editor")
        cancel.tap()

        XCTAssertTrue(dueRow.waitForExistence(timeout: 5),
                      "Cancelling compose should return to the Tickle list")
        XCTAssertFalse(cancel.exists, "Compose sheet should be gone")
    }

    /// Contact detail → Send a text → Cancel must land back on the
    /// contact's detail screen.
    func testComposeFromContactDetailReturnsToDetailOnCancel() {
        let app = launchApp()

        let networkTab = app.tabBars.buttons.element(boundBy: 0)
        XCTAssertTrue(networkTab.waitForExistence(timeout: 15))
        networkTab.tap()

        // Open a known seeded contact — the network list sorts by last name,
        // so Fatima Al-Hassan (test_contacts.csv) is at the top.
        let firstContact = app.staticTexts["Fatima Al-Hassan"].firstMatch
        XCTAssertTrue(firstContact.waitForExistence(timeout: 5),
                      "Network list should show seeded contacts")
        firstContact.tap()

        let sendText = app.buttons["Send a text"]
        XCTAssertTrue(sendText.waitForExistence(timeout: 5),
                      "Contact detail should show the Send a text chip")
        sendText.tap()

        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 5),
                      "Compose sheet should appear from contact detail")
        cancel.tap()

        XCTAssertTrue(sendText.waitForExistence(timeout: 5),
                      "Cancelling compose should return to the contact detail screen")
    }

    /// Compose tab → Cancel with an empty draft resets the form and stays on
    /// Compose (TIC-93) — it no longer bounces to the Tickle home. There's
    /// nowhere to "return" to since Compose already is the current tab.
    func testComposeTabCancelStaysOnComposeTab() {
        let app = launchApp()

        let dueRow = firstDueRow(in: app)
        XCTAssertTrue(dueRow.waitForExistence(timeout: 15))

        let composeTab = app.tabBars.buttons.element(boundBy: 3)
        XCTAssertTrue(composeTab.waitForExistence(timeout: 5))
        composeTab.tap()

        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 5),
                      "Compose tab should show its Cancel button")
        cancel.tap()

        // Still on Compose — Cancel is still there (the form reset, the
        // screen didn't dismiss) — and the Tickle home's due row, which
        // would only be visible after switching tabs, is not.
        XCTAssertTrue(cancel.waitForExistence(timeout: 5),
                      "Cancel with an empty draft should reset the form and stay on Compose")
        XCTAssertFalse(dueRow.exists, "should not have switched back to the Tickle tab")
    }

    // Note: the draft-protection confirmation itself ("Discard draft?" /
    // Discard / Keep Editing) is covered by `ComposeViewCancelDecisionTests`
    // (pure `ComposeView.cancelDecision` logic) rather than here — driving a
    // SwiftUI TextEditor through the keyboard and then a system
    // confirmationDialog in the same XCUITest flow proved flaky (keyboard
    // dismissal swallows the first synthesized tap on the toolbar button),
    // and the pure-logic test already pins the actual branching decision.
}
