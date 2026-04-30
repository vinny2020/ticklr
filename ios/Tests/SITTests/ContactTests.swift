import XCTest
@testable import Ticklr

final class ContactTests: XCTestCase {
    // Commit test 3

    // MARK: - fullName

    func testFullNameBothNames() {
        let contact = Contact(firstName: "John", lastName: "Doe")
        XCTAssertEqual(contact.fullName, "John Doe")
    }

    func testFullNameFirstNameOnly() {
        let contact = Contact(firstName: "John", lastName: "")
        XCTAssertEqual(contact.fullName, "John")
    }

    func testFullNameLastNameOnly() {
        let contact = Contact(firstName: "", lastName: "Doe")
        XCTAssertEqual(contact.fullName, "Doe")
    }

    func testFullNameBothEmpty() {
        let contact = Contact(firstName: "", lastName: "")
        XCTAssertEqual(contact.fullName, "")
    }

    func testFullNameTrimsExtraWhitespace() {
        let contact = Contact(firstName: "  Alice  ", lastName: "  ")
        // trimmingCharacters only trims the joined result, not inner spaces
        let result = contact.fullName
        XCTAssertFalse(result.hasPrefix(" "))
        XCTAssertFalse(result.hasSuffix(" "))
    }

    // MARK: - initials

    func testInitialsBothNames() {
        let contact = Contact(firstName: "John", lastName: "Doe")
        XCTAssertEqual(contact.initials, "JD")
    }

    func testInitialsFirstNameOnly() {
        let contact = Contact(firstName: "Alice", lastName: "")
        XCTAssertEqual(contact.initials, "A")
    }

    func testInitialsLastNameOnly() {
        let contact = Contact(firstName: "", lastName: "Brown")
        XCTAssertEqual(contact.initials, "B")
    }

    func testInitialsAreLowercaseInputUppercased() {
        let contact = Contact(firstName: "alice", lastName: "brown")
        XCTAssertEqual(contact.initials, "AB")
    }

    func testInitialsBothEmpty() {
        let contact = Contact(firstName: "", lastName: "")
        XCTAssertEqual(contact.initials, "?")
    }

    // MARK: - Default values

    func testDefaultImportSourceIsManual() {
        let contact = Contact(firstName: "Jane", lastName: "Doe")
        XCTAssertEqual(contact.importSource, .manual)
    }

    func testDefaultPhoneNumbersIsEmpty() {
        let contact = Contact(firstName: "Jane", lastName: "Doe")
        XCTAssertTrue(contact.phoneNumbers.isEmpty)
    }

    func testDefaultEmailsIsEmpty() {
        let contact = Contact(firstName: "Jane", lastName: "Doe")
        XCTAssertTrue(contact.emails.isEmpty)
    }

    func testDefaultNotesIsEmpty() {
        let contact = Contact(firstName: "Jane", lastName: "Doe")
        XCTAssertTrue(contact.notes.isEmpty)
    }

    func testDefaultTagsIsEmpty() {
        let contact = Contact(firstName: "Jane", lastName: "Doe")
        XCTAssertTrue(contact.tags.isEmpty)
    }

    // MARK: - Custom values

    func testPhoneNumbersStoredCorrectly() {
        let contact = Contact(firstName: "Bob", lastName: "Smith", phoneNumbers: ["555-000-1111", "555-000-2222"])
        XCTAssertEqual(contact.phoneNumbers.count, 2)
        XCTAssertEqual(contact.phoneNumbers[0], "555-000-1111")
        XCTAssertEqual(contact.phoneNumbers[1], "555-000-2222")
    }

    func testEmailsStoredCorrectly() {
        let contact = Contact(firstName: "Bob", lastName: "Smith", emails: ["bob@work.com", "bob@home.com"])
        XCTAssertEqual(contact.emails.count, 2)
    }

    func testLinkedInImportSource() {
        let contact = Contact(firstName: "Bob", lastName: "Smith", importSource: .linkedin)
        XCTAssertEqual(contact.importSource, .linkedin)
    }
}
