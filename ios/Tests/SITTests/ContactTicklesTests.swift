import XCTest
import SwiftData
@testable import Ticklr

@MainActor
final class ContactTicklesTests: XCTestCase {
    func testProfileQueryIsScopedSortedAndReflectsMutations() throws {
        let container = try ModelContainer(for: Contact.self, ContactGroup.self, TickleReminder.self,
                                           configurations: ModelConfiguration(isStoredInMemoryOnly: true))
        let context = container.mainContext
        let alice = Contact(firstName: "Alice", lastName: "Doe")
        let bob = Contact(firstName: "Bob", lastName: "Doe")
        let group = ContactGroup(name: "Friends")
        context.insert(alice)
        context.insert(bob)
        context.insert(group)
        let early = TickleReminder(contact: alice, note: "Early", startDate: Date(timeIntervalSince1970: 100))
        let later = TickleReminder(contact: alice, note: "Later", startDate: Date(timeIntervalSince1970: 200))
        early.nextDueDate = Date(timeIntervalSince1970: 100)
        later.nextDueDate = Date(timeIntervalSince1970: 200)
        later.status = .snoozed
        let other = TickleReminder(contact: bob, note: "Other person")
        let groupTickle = TickleReminder(group: group, note: "Group only")
        let completed = TickleReminder(contact: alice, note: "History", frequency: .oneTime)
        completed.status = .completed
        for reminder in [later, other, groupTickle, completed, early] { context.insert(reminder) }
        try context.save()

        func profile() throws -> [TickleReminder] {
            ContactTicklesSection.scheduled(try context.fetch(ContactTicklesSection.descriptor(contactID: alice.id)))
        }
        XCTAssertEqual(try profile().map(\.note), ["Early", "Later"])
        early.note = "Edited"
        try context.save()
        XCTAssertEqual(try profile().first?.note, "Edited")
        early.status = .completed
        try context.save()
        XCTAssertEqual(try profile().map(\.id), [later.id])
        later.contact = bob
        try context.save()
        XCTAssertTrue(try profile().isEmpty)
        let added = TickleReminder(contact: alice, note: "New")
        context.insert(added)
        try context.save()
        XCTAssertEqual(try profile().map(\.id), [added.id])
        context.delete(added)
        try context.save()
        XCTAssertTrue(try profile().isEmpty)
    }
}
