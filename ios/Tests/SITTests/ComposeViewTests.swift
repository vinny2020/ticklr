import XCTest
import SwiftData
import ViewInspector
@testable import Ticklr

/// Wiring tests for `ComposeView`'s Cancel button.
///
/// ComposeView is presented from two contexts and each injects a different
/// `onClose` closure:
///   • From the Compose tab (`ContentView`): closure switches `selectedTab`
///     back to `.tickle` (the new home).
///   • From `ContactDetailView` (sheet presentation): closure dismisses the
///     sheet, returning the user to the contact's detail screen.
///
/// These tests pin the Cancel button → onClose closure wiring at the view
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
        let view = ComposeView(onClose: { cancelCount += 1 })
            .modelContainer(container)

        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.cancel")).tap()

        XCTAssertEqual(cancelCount, 1, "Cancel must fire onClose even when the form is empty")
    }

    func testCancelTapInvokesOnCancelWithPreselectedContact() throws {
        // Models the contact → compose → cancel flow. ContactDetailView
        // presents ComposeView as a sheet with `initialContact: contact` and
        // `onClose: { activeSheet = nil }`. We can't easily test "the sheet
        // was dismissed" at this layer (that's a parent-state mutation), but
        // we CAN verify the Cancel button still invokes the closure when the
        // form is pre-populated — which is the wiring contract the parent
        // depends on.
        let contact = Contact(firstName: "Alice", lastName: "Doe")
        var cancelCount = 0
        let view = ComposeView(
            onClose: { cancelCount += 1 },
            initialContact: contact
        )
        .modelContainer(container)

        let sut = try view.inspect()
        try sut.find(button: String(localized: "common.cancel")).tap()

        XCTAssertEqual(cancelCount, 1, "Cancel must fire onClose regardless of form state")
    }

    // MARK: - TIC-90: template menu affordances

    func testEmptyTemplatesShowsCreateTemplateEntry() throws {
        // No templates saved yet — the menu must offer a way in rather than
        // disappearing (the bug this ticket fixes).
        let view = ComposeView().modelContainer(container)
        let sut = try view.inspect()

        XCTAssertNoThrow(try sut.find(button: String(localized: "compose.template.create")),
                         "zero templates must surface a 'Create a template…' entry")
    }
}

/// TIC-90: pure decision logic for whether picking a template needs to
/// confirm before overwriting a hand-typed draft. Picking a template must
/// never silently clobber text the user actually typed, but switching
/// templates (or picking one into an empty/untouched body) should stay a
/// single tap.
@MainActor
final class ComposeViewTemplateReplaceDecisionTests: XCTestCase {

    func testAppliesDirectlyWhenBodyIsEmpty() {
        XCTAssertEqual(
            ComposeView.templateReplaceDecision(currentBody: "", appliedTemplateBody: nil),
            .applyDirectly,
            "an empty draft has nothing to protect"
        )
    }

    func testAppliesDirectlyWhenBodyIsWhitespaceOnly() {
        XCTAssertEqual(
            ComposeView.templateReplaceDecision(currentBody: "   ", appliedTemplateBody: nil),
            .applyDirectly,
            "whitespace-only text is treated the same as empty (matches canSend's trim rule)"
        )
    }

    func testAppliesDirectlyWhenBodyStillEqualsAppliedTemplate() {
        XCTAssertEqual(
            ComposeView.templateReplaceDecision(currentBody: "Hey! Thinking of you.", appliedTemplateBody: "Hey! Thinking of you."),
            .applyDirectly,
            "an untouched, previously-applied template body isn't a user draft — switching needs no confirmation"
        )
    }

    func testConfirmsWhenBodyDiffersFromAppliedTemplate() {
        XCTAssertEqual(
            ComposeView.templateReplaceDecision(currentBody: "Hey! Thinking of you, and also...", appliedTemplateBody: "Hey! Thinking of you."),
            .confirmReplace,
            "edited text derived from a template is still a draft worth protecting"
        )
    }

    func testConfirmsWhenNoTemplateAppliedYetAndBodyIsHandTyped() {
        XCTAssertEqual(
            ComposeView.templateReplaceDecision(currentBody: "Just calling to say hi", appliedTemplateBody: nil),
            .confirmReplace,
            "hand-typed text with no template applied is exactly the draft this feature protects"
        )
    }
}
