import SwiftUI
import SwiftData

struct GroupDetailView: View {
    @Bindable var group: ContactGroup
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var allContacts: [Contact]

    @State private var searchText = ""
    @State private var showingAddMembers = false
    @State private var showingEditGroup = false

    var members: [Contact] {
        let base = group.contacts.sorted { $0.lastName < $1.lastName }
        guard !searchText.isEmpty else { return base }
        return base.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        List(members) { contact in
            NavigationLink(destination: ContactDetailView(contact: contact)) {
                ContactRowView(contact: contact)
            }
            .swipeActions(edge: .trailing) {
                Button(role: .destructive) {
                    group.contacts.removeAll(where: { $0.id == contact.id })
                    try? modelContext.save()
                } label: {
                    Label("Remove", systemImage: "person.badge.minus")
                }
            }
        }
        .searchable(text: $searchText, prompt: "Search \(group.name)")
        .navigationTitle("\(group.emoji) \(group.name)")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        showingAddMembers = true
                    } label: {
                        Label("Add Members", systemImage: "person.badge.plus")
                    }
                    Button {
                        showingEditGroup = true
                    } label: {
                        Label("Edit Group", systemImage: "pencil")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .overlay {
            if group.contacts.isEmpty {
                ContentUnavailableView(
                    "No members yet",
                    systemImage: "person.badge.plus",
                    description: Text("Tap ··· to add contacts to this group")
                )
            } else if !searchText.isEmpty && members.isEmpty {
                ContentUnavailableView.search
            }
        }
        .sheet(isPresented: $showingAddMembers) {
            AddMembersSheet(group: group)
        }
        .sheet(isPresented: $showingEditGroup) {
            GroupEditSheet(group: group)
        }
    }
}

// MARK: - Add Members Sheet

private struct AddMembersSheet: View {
    @Bindable var group: ContactGroup
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Contact.lastName) private var allContacts: [Contact]

    @State private var searchText = ""

    var nonMembers: [Contact] {
        let memberIDs = Set(group.contacts.map { $0.id })
        let base = allContacts.filter { !memberIDs.contains($0.id) }
        guard !searchText.isEmpty else { return base }
        return base.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        NavigationStack {
            List(nonMembers) { contact in
                Button {
                    group.contacts.append(contact)
                    try? modelContext.save()
                } label: {
                    HStack {
                        ContactRowView(contact: contact)
                        Spacer()
                        Image(systemName: "plus.circle")
                            .foregroundStyle(.indigo)
                    }
                }
                .tint(.primary)
            }
            .searchable(text: $searchText, prompt: "Search contacts")
            .navigationTitle("Add to \(group.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .overlay {
                if nonMembers.isEmpty && searchText.isEmpty {
                    ContentUnavailableView(
                        "Everyone's in this group",
                        systemImage: "checkmark.circle",
                        description: Text("All contacts are already members")
                    )
                } else if nonMembers.isEmpty {
                    ContentUnavailableView.search
                }
            }
        }
    }
}
