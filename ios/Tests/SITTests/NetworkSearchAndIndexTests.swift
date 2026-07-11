import XCTest
import SwiftData
@testable import Ticklr

/// TIC-92: Network list broadened search + A–Z section index. Covers the two
/// pure decision helpers promoted out of `NetworkListView` — `ContactSearchFilter`
/// (name/company/phone/tags/notes, case-insensitive) and `ContactSectionIndex`
/// (letter bucketing, non-letter → "#", ordering, filter interaction).
@MainActor
final class NetworkSearchAndIndexTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!

    override func setUp() async throws {
        container = try ModelContainer(
            for: TickleReminder.self, Contact.self, ContactGroup.self, MessageTemplate.self,
            configurations: .init(isStoredInMemoryOnly: true)
        )
        context = container.mainContext
    }

    override func tearDown() {
        container = nil
        context = nil
    }

    // MARK: - Search predicate

    func testEmptyQueryMatchesEverything() {
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Ada Lovelace", company: "", phoneNumbers: [], tags: [], notes: "", query: ""))
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Ada Lovelace", company: "", phoneNumbers: [], tags: [], notes: "", query: "   "))
    }

    func testMatchesNameCaseInsensitive() {
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Ada Lovelace", company: "Analytical Engines",
            phoneNumbers: [], tags: [], notes: "", query: "LOVE"))
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Ada Lovelace", company: "",
            phoneNumbers: [], tags: [], notes: "", query: "ada"))
    }

    func testMatchesCompany() {
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Bob", company: "Acme Corp",
            phoneNumbers: [], tags: [], notes: "", query: "acme"))
    }

    func testMatchesNotes() {
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Bob", company: "",
            phoneNumbers: [], tags: [], notes: "Met at the jazz festival", query: "JAZZ"))
    }

    func testMatchesTags() {
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Bob", company: "",
            phoneNumbers: [], tags: ["climbing", "mentor"], notes: "", query: "Mentor"))
    }

    func testPhoneMatchIgnoresFormatting() {
        // Query digits found inside a formatted stored number.
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Carol", company: "",
            phoneNumbers: ["+1 (555) 123-4567"], tags: [], notes: "", query: "5551234"))
        // Formatted query against plain stored digits.
        XCTAssertTrue(ContactSearchFilter.matches(
            name: "Carol", company: "",
            phoneNumbers: ["5551234567"], tags: [], notes: "", query: "(555) 123"))
    }

    func testTextQueryDoesNotSpuriouslyMatchPhone() {
        // A non-digit query must not fall through to phone matching.
        XCTAssertFalse(ContactSearchFilter.matches(
            name: "Carol", company: "",
            phoneNumbers: ["5551234567"], tags: [], notes: "", query: "zzz"))
    }

    func testNoMatchReturnsFalse() {
        XCTAssertFalse(ContactSearchFilter.matches(
            name: "Ada Lovelace", company: "Acme",
            phoneNumbers: ["5551234567"], tags: ["mentor"], notes: "jazz", query: "quantum"))
    }

    func testNormalizedDigitsStripsNonDigits() {
        XCTAssertEqual(ContactSearchFilter.normalizedDigits("+1 (555) 123-4567"), "15551234567")
        XCTAssertEqual(ContactSearchFilter.normalizedDigits("no digits"), "")
    }

    func testMatchesLiveContactOverload() {
        let c = Contact(firstName: "Grace", lastName: "Hopper",
                        phoneNumbers: ["617-555-0100"], company: "Navy",
                        notes: "COBOL", tags: ["legend"])
        context.insert(c)
        XCTAssertTrue(ContactSearchFilter.matches(c, query: "hopper"))
        XCTAssertTrue(ContactSearchFilter.matches(c, query: "cobol"))
        XCTAssertTrue(ContactSearchFilter.matches(c, query: "legend"))
        XCTAssertTrue(ContactSearchFilter.matches(c, query: "6175550100"))
        XCTAssertFalse(ContactSearchFilter.matches(c, query: "fortran"))
    }

    // MARK: - Section bucketing

    func testSectionLetterUppercasesFirstLetter() {
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "adams"), "A")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "Zimmer"), "Z")
    }

    func testSectionLetterFoldsDiacritics() {
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: " Órla"), "O")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "ñoño"), "N")
    }

    func testSectionLetterNonLetterBucketsToHash() {
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "123 Co"), "#")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "+Plus"), "#")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "🎉 party"), "#")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: ""), "#")
        XCTAssertEqual(ContactSectionIndex.sectionLetter(for: "   "), "#")
    }

    func testSortKeyFallsBackToFirstNameWhenLastBlank() {
        XCTAssertEqual(ContactSectionIndex.sortKey(lastName: "Doe", firstName: "Jane"), "Doe")
        XCTAssertEqual(ContactSectionIndex.sortKey(lastName: "", firstName: "Cher"), "Cher")
        XCTAssertEqual(ContactSectionIndex.sortKey(lastName: "   ", firstName: "Cher"), "Cher")
    }

    // MARK: - Section assembly

    func testSectionsAreAlphabeticalWithHashLast() {
        let keys = ["Baker", "adams", "9Lives", "Carter", "!bang"]
        let sections = ContactSectionIndex.sections(from: keys) { $0 }
        XCTAssertEqual(sections.map(\.letter), ["A", "B", "C", "#"])
    }

    func testSectionsGroupMembersUnderTheirLetter() {
        let keys = ["adams", "Aldrin", "Baker"]
        let sections = ContactSectionIndex.sections(from: keys) { $0 }
        let a = sections.first { $0.letter == "A" }
        XCTAssertEqual(a?.items.count, 2)
        // Sorted case-insensitively within the section.
        XCTAssertEqual(a?.items, ["adams", "Aldrin"])
        XCTAssertEqual(sections.first { $0.letter == "B" }?.items, ["Baker"])
    }

    func testSectionsFromContactsUseLastNameFallback() {
        let a = Contact(firstName: "Ann", lastName: "Zephyr")   // → Z
        let b = Contact(firstName: "Bono", lastName: "")        // → B (first-name fallback)
        let c = Contact(firstName: "9", lastName: "")           // → #
        [a, b, c].forEach { context.insert($0) }

        let sections = ContactSectionIndex.sections(from: [a, b, c])
        XCTAssertEqual(sections.map(\.letter), ["B", "Z", "#"])
        XCTAssertEqual(sections.first { $0.letter == "B" }?.items.first?.id, b.id)
        XCTAssertEqual(sections.first { $0.letter == "Z" }?.items.first?.id, a.id)
    }

    // MARK: - Filter × sectioning interaction

    func testSectionsRespectAnUpstreamGroupFilter() {
        let team = ContactGroup(name: "Team")
        context.insert(team)
        let amy  = Contact(firstName: "Amy", lastName: "Anders")
        let ben  = Contact(firstName: "Ben", lastName: "Barnes")
        let cleo = Contact(firstName: "Cleo", lastName: "Carr")
        [amy, ben, cleo].forEach { context.insert($0) }
        amy.groups.append(team)
        cleo.groups.append(team)
        try? context.save()

        // Mirror the view pipeline: filter first, then section.
        let filtered = GroupMembershipFilter.contacts([amy, ben, cleo], inGroupWithID: team.id)
        let sections = ContactSectionIndex.sections(from: filtered)

        XCTAssertEqual(sections.map(\.letter), ["A", "C"])
        XCTAssertFalse(sections.contains { $0.items.contains { $0.id == ben.id } },
                       "A non-member must not appear in any section")
    }

    func testSectionsRespectAnUpstreamSearchFilter() {
        let ada   = Contact(firstName: "Ada", lastName: "Adams", notes: "jazz")
        let bob   = Contact(firstName: "Bob", lastName: "Barnes", notes: "climbing")
        [ada, bob].forEach { context.insert($0) }

        let filtered = [ada, bob].filter { ContactSearchFilter.matches($0, query: "jazz") }
        let sections = ContactSectionIndex.sections(from: filtered)

        XCTAssertEqual(sections.map(\.letter), ["A"])
        XCTAssertEqual(sections.first?.items.first?.id, ada.id)
    }
}
