import XCTest
import SwiftData
@testable import Ticklr

@MainActor
final class LinkedInCSVParserTests: XCTestCase {

    var container: ModelContainer!
    var context: ModelContext!

    override func setUpWithError() throws {
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        container = try ModelContainer(
            for: Contact.self, ContactGroup.self, TickleReminder.self,
            configurations: config
        )
        context = ModelContext(container)
    }

    override func tearDown() {
        container = nil
        context = nil
    }

    // MARK: - Helpers

    func writeTempCSV(_ content: String) throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString + ".csv")
        try content.write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    func fetchContacts() throws -> [Contact] {
        try context.fetch(FetchDescriptor<Contact>())
    }

    // MARK: - Tests

    func testParsesBasicRow() throws {
        let csv = "First Name,Last Name,Email Address,Company,Position\nJohn,Doe,john@example.com,Acme,Engineer"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts.count, 1)
        XCTAssertEqual(contacts[0].firstName, "John")
        XCTAssertEqual(contacts[0].lastName, "Doe")
        XCTAssertEqual(contacts[0].emails, ["john@example.com"])
        XCTAssertEqual(contacts[0].company, "Acme")
        XCTAssertEqual(contacts[0].jobTitle, "Engineer")
        XCTAssertEqual(contacts[0].importSource, .linkedin)
    }

    func testSkipsMetadataLinesBeforeHeader() throws {
        let csv = "Notes: LinkedIn Export\nGenerated: 2024-01-01\nFirst Name,Last Name,Email Address,Company,Position\nJane,Smith,jane@example.com,Corp,Manager"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts.count, 1)
        XCTAssertEqual(contacts[0].firstName, "Jane")
    }

    func testThrowsWhenNoHeader() throws {
        let csv = "no,valid,header,here\nfoo,bar,baz"
        let url = try writeTempCSV(csv)
        XCTAssertThrowsError(try LinkedInCSVParser.parse(url: url, context: context)) { error in
            XCTAssertEqual(error as? LinkedInCSVParser.CSVError, .noHeaderFound)
        }
    }

    func testEmptyBodyProducesNoContacts() throws {
        let url = try writeTempCSV("First Name,Last Name\n")
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts.count, 0)
    }

    func testParsesQuotedFieldContainingComma() throws {
        let csv = "First Name,Last Name,Email Address,Company,Position\nAlice,Wang,alice@example.com,\"Widgets, Inc.\",Senior Engineer"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts[0].company, "Widgets, Inc.")
    }

    func testParsesMultipleContacts() throws {
        let csv = "First Name,Last Name,Email Address,Company,Position\nAlice,Wang,alice@example.com,Acme,Engineer\nBob,Jones,bob@example.com,Corp,Designer\nCarol,Smith,carol@example.com,LLC,Manager"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts.count, 3)
    }

    func testHandlesMissingOptionalFields() throws {
        let csv = "First Name,Last Name\nBob,Jones"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts.count, 1)
        XCTAssertEqual(contacts[0].emails, [])
        XCTAssertEqual(contacts[0].company, "")
    }

    func testParsesPhoneNumber() throws {
        let csv = "First Name,Last Name,Email Address,Company,Position,Phone Number\nDave,Brown,dave@example.com,Corp,Dev,555-123-4567"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts[0].phoneNumbers, ["555-123-4567"])
    }

    func testEmptyPhoneNumberIsNotStored() throws {
        let csv = "First Name,Last Name,Email Address,Company,Position,Phone Number\nEve,Adams,eve@example.com,Corp,PM,"
        let url = try writeTempCSV(csv)
        try LinkedInCSVParser.parse(url: url, context: context)
        let contacts = try fetchContacts()
        XCTAssertEqual(contacts[0].phoneNumbers, [])
    }
}
