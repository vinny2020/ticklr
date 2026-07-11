import XCTest
import SwiftData
@testable import Ticklr

/// TIC-88: group-level actions. Covers the pure decision logic promoted out of
/// the views — group-tickle notification naming, the Network group-filter
/// membership predicate, and the create-with-members flow state machine.
@MainActor
final class GroupFeatureTests: XCTestCase {

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

    // MARK: - Group-tickle notification naming

    func testNotificationNameUsesGroupNameForGroupTickle() {
        // A group-anchored reminder (contact nil, group set) surfaces the
        // group's name in its notification title — the fallback TickleScheduler
        // relies on for group tickles.
        let group = ContactGroup(name: "Hiking Crew")
        context.insert(group)
        let reminder = TickleReminder(contact: nil, group: group, note: "plan a hike", frequency: .monthly)
        context.insert(reminder)

        XCTAssertNil(reminder.contact)
        XCTAssertEqual(reminder.group?.id, group.id)
        XCTAssertEqual(TickleScheduler.notificationDisplayName(for: reminder), "Hiking Crew")
    }

    func testNotificationNamePrefersContactWhenContactAnchored() {
        let contact = Contact(firstName: "Alice", lastName: "Doe")
        context.insert(contact)
        let reminder = TickleReminder(contact: contact, note: "hi", frequency: .monthly)
        context.insert(reminder)

        XCTAssertEqual(TickleScheduler.notificationDisplayName(for: reminder), "Alice Doe")
    }

    func testNotificationNameFallsBackWhenUntargeted() {
        let reminder = TickleReminder(note: "orphan", frequency: .monthly)
        context.insert(reminder)

        XCTAssertEqual(TickleScheduler.notificationDisplayName(for: reminder), "someone")
    }

    // MARK: - Network group-filter predicate

    func testGroupMembershipFilterReturnsOnlyMembers() {
        let group = ContactGroup(name: "Team")
        context.insert(group)
        let alice = Contact(firstName: "Alice", lastName: "A")
        let bob   = Contact(firstName: "Bob", lastName: "B")
        let cara  = Contact(firstName: "Cara", lastName: "C")
        [alice, bob, cara].forEach { context.insert($0) }
        alice.groups.append(group)
        cara.groups.append(group)
        try? context.save()

        let result = GroupMembershipFilter.contacts([alice, bob, cara], inGroupWithID: group.id)

        XCTAssertEqual(Set(result.map { $0.id }), Set([alice.id, cara.id]))
        XCTAssertFalse(result.contains { $0.id == bob.id }, "A non-member must be excluded")
    }

    func testGroupMembershipFilterEmptyForUnknownGroup() {
        let alice = Contact(firstName: "Alice", lastName: "A")
        context.insert(alice)

        let result = GroupMembershipFilter.contacts([alice], inGroupWithID: UUID())

        XCTAssertTrue(result.isEmpty, "No contact belongs to a group id that doesn't exist")
    }

    func testGroupMembershipFilterDistinguishesBetweenGroups() {
        let g1 = ContactGroup(name: "One")
        let g2 = ContactGroup(name: "Two")
        context.insert(g1); context.insert(g2)
        let alice = Contact(firstName: "Alice", lastName: "A")
        context.insert(alice)
        alice.groups.append(g1)
        try? context.save()

        XCTAssertEqual(GroupMembershipFilter.contacts([alice], inGroupWithID: g1.id).count, 1)
        XCTAssertTrue(GroupMembershipFilter.contacts([alice], inGroupWithID: g2.id).isEmpty,
                      "Membership in g1 must not match a filter for g2")
    }

    // MARK: - Create-with-members flow

    func testFlowPromotesCreatedGroupOnlyAfterCreateSheetDismisses() {
        let group = ContactGroup(name: "New Circle")
        context.insert(group)
        var flow = GroupCreationFlow()

        XCTAssertNil(flow.groupToPopulate)

        flow.groupCreated(group)
        // Parked while the create sheet is still on screen — must NOT present
        // Add Members yet (stacking sheets mid-dismiss drops one).
        XCTAssertNil(flow.groupToPopulate)
        XCTAssertEqual(flow.pendingCreatedGroup?.id, group.id)

        flow.createSheetDismissed()
        // Now promoted to the Add Members slot.
        XCTAssertEqual(flow.groupToPopulate?.id, group.id)
        XCTAssertNil(flow.pendingCreatedGroup)
    }

    func testCancelledCreateDoesNotOpenAddMembers() {
        var flow = GroupCreationFlow()

        flow.createSheetDismissed()   // sheet dismissed without creating a group

        XCTAssertNil(flow.groupToPopulate, "Cancelling create must not open Add Members")
        XCTAssertNil(flow.pendingCreatedGroup)
    }

    func testPopulateFinishedClearsSlot() {
        let group = ContactGroup(name: "Circle")
        context.insert(group)
        var flow = GroupCreationFlow()
        flow.groupCreated(group)
        flow.createSheetDismissed()
        XCTAssertNotNil(flow.groupToPopulate)

        flow.populateFinished()

        XCTAssertNil(flow.groupToPopulate)
    }
}
