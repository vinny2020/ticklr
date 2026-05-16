import XCTest
@testable import Ticklr

final class WarmCategoryTests: XCTestCase {

    // MARK: - Stable UUIDs

    /// CRITICAL: these UUIDs are committed to user installs. They MUST NOT
    /// change once shipped — every CanonicalGroupSeed run keys off them.
    func testCanonicalUUIDsAreStable() {
        let expected: [(WarmCategory, String)] = [
            (.family,     "CA2C12E0-0FAD-4019-A681-0EC31436D02B"),
            (.friends,    "B03D94E2-FAAD-4F9F-970E-DDAEDB1415A9"),
            (.work,       "20C1A7D7-293C-437D-BD20-DFAE01AF78EB"),
            (.milestones, "7FFDC83B-6713-4314-9B70-7971833E2D79"),
            (.community,  "1850BD29-904F-430F-8DBC-1B7AF15836C9"),
        ]
        for (category, expectedString) in expected {
            XCTAssertEqual(category.groupUUID.uuidString, expectedString,
                           "\(category.rawValue) canonical UUID drifted — DO NOT change, this breaks every existing install")
        }
    }

    func testAllCanonicalUUIDsAreUnique() {
        let uuids = WarmCategory.allCases.map { $0.groupUUID }
        XCTAssertEqual(uuids.count, Set(uuids).count, "canonical UUIDs collided")
    }

    func testSortOrderMatchesDeclaration() {
        let ordered = WarmCategory.allCases.sorted { $0.sortOrder < $1.sortOrder }
        XCTAssertEqual(ordered, [.family, .friends, .work, .milestones, .community])
    }

    func testFromGroupIdResolvesCanonical() {
        for category in WarmCategory.allCases {
            XCTAssertEqual(WarmCategory.from(groupId: category.groupUUID), category)
        }
    }

    func testFromGroupIdReturnsNilForRandomUUID() {
        XCTAssertNil(WarmCategory.from(groupId: UUID()))
    }

    // MARK: - Resolver (contact → category)

    func testResolveFallsBackToCommunityForNoGroups() {
        let contact = makeContact()
        XCTAssertEqual(WarmCategory.resolve(for: contact), .community)
    }

    func testResolveOptionalReturnsNilForNoCanonicalGroups() {
        let contact = makeContact()
        XCTAssertNil(WarmCategory.resolveOptional(for: contact))
    }

    func testResolveOptionalReturnsNilWhenOnlyUserCreatedGroups() {
        let contact = makeContact()
        contact.groups = [ContactGroup(name: "Pickleball Crew", emoji: "🥒")]
        XCTAssertNil(WarmCategory.resolveOptional(for: contact))
    }

    func testResolveOptionalPicksLastCanonicalInArray() {
        let contact = makeContact()
        let family = ContactGroup(name: "Family", emoji: "👨‍👩‍👧", id: WarmCategory.family.groupUUID)
        let work = ContactGroup(name: "Work", emoji: "💼", id: WarmCategory.work.groupUUID)
        contact.groups = [family, work]
        XCTAssertEqual(WarmCategory.resolveOptional(for: contact), .work,
                       "should pick last canonical (most recently added) when multiple present")
    }

    func testResolveIgnoresUserGroupsBetweenCanonicals() {
        let contact = makeContact()
        let family = ContactGroup(name: "Family", emoji: "👨‍👩‍👧", id: WarmCategory.family.groupUUID)
        let userGroup = ContactGroup(name: "Pickleball Crew", emoji: "🥒")
        contact.groups = [family, userGroup]
        XCTAssertEqual(WarmCategory.resolveOptional(for: contact), .family,
                       "user-created groups in the array should not mask the canonical match")
    }

    // MARK: - Palette + localized copy sanity

    func testEveryCategoryExposesNonEmptyLocalizedStrings() {
        for category in WarmCategory.allCases {
            XCTAssertFalse(category.localizedLabel.isEmpty, "\(category) label missing")
            XCTAssertFalse(category.localizedGroupName.isEmpty, "\(category) groupName missing")
            XCTAssertFalse(category.localizedHeadlineLine1.isEmpty, "\(category) headline.line1 missing")
            XCTAssertFalse(category.localizedHeadlineLine2.isEmpty, "\(category) headline.line2 missing")
            XCTAssertFalse(category.localizedBody.isEmpty, "\(category) body missing")
            XCTAssertFalse(category.localizedPrompt.isEmpty, "\(category) prompt missing")
            XCTAssertFalse(category.localizedPromptShort.isEmpty, "\(category) promptShort missing")
            XCTAssertFalse(category.systemImageName.isEmpty, "\(category) SF symbol missing")
            XCTAssertFalse(category.defaultEmoji.isEmpty, "\(category) default emoji missing")
        }
    }

    // MARK: - Helpers

    private func makeContact() -> Contact {
        Contact(firstName: "Test", lastName: "User")
    }
}
