import XCTest
import SwiftData
import ViewInspector
@testable import Ticklr

/// Wiring tests for `ComposeView`'s Cancel button.
///
/// ComposeView is presented from two contexts and each injects a different
/// `onCancel` closure:
///   • From the Compose tab (`ContentView`): closure switches `selectedTab`
///     back to `.tickle` (the new home).
///   • From `ContactDetailView` (sheet presentation): closure dismisses the
///     sheet, returning the user to the contact's detail screen.
///
/// These tests pin the Cancel button → onCancel closure wiring at the view
/// level. Whichever destination the parent wants is its own concern.
@MainActor
final class ComposeViewTests: XCTestCase {

    private var container: ModelContainer!

    override func setUp() async throws {
        // ComposeView declares @Query on Contact and MessageTemplate; provide
        // an in-memory container so the body doesn't fault when inspected.
        container = try ModelContainer(
            for: TickleReminder.self, Contact.self, ContactGroup.self, MessageTemplate.self,
            configurations: .init(isStoredInMemoryOnly: true)
        )
    }

    func testCancelTapInvokesOnCancelWithEmptyForm() throws {
        // The empty-form case is the regression that commit 542a3e0 fixed —
        // the Cancel button used to be gated behind `selectedContact != nil
        // || !messageBody.isEmpty`, so a user landing on Compose with nothing
        // typed had no way out except tapping another tab.
        var cancelCount = 0
        let view = ComposeView(onCancel: { cancelCount += 1 })
            .modelContainer(container)

        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.cancel")).tap()

        XCTAssertEqual(cancelCount, 1, "Cancel must fire onCancel even when the form is empty")
    }

    func testCancelTapInvokesOnCancelWithPreselectedContact() throws {
        // Models the contact → compose → cancel flow. ContactDetailView
        // presents ComposeView as a sheet with `initialContact: contact` and
        // `onCancel: { activeSheet = nil }`. We can't easily test "the sheet
        // was dismissed" at this layer (that's a parent-state mutation), but
        // we CAN verify the Cancel button still invokes the closure when the
        // form is pre-populated — which is the wiring contract the parent
        // depends on.
        let contact = Contact(firstName: "Alice", lastName: "Doe")
        var cancelCount = 0
        let view = ComposeView(
            onCancel: { cancelCount += 1 },
            initialContact: contact
        )
        .modelContainer(container)

        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.cancel")).tap()

        XCTAssertEqual(cancelCount, 1, "Cancel must fire onCancel regardless of form state")
    }
}
