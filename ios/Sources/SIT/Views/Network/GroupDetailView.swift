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
                    Label(String(localized: "groupDetail.action.remove"), systemImage: "person.badge.minus")
                }
            }
        }
        .searchable(text: $searchText, prompt: String(localized: "groupDetail.search.prompt \(group.name)"))
        .navigationTitle("\(group.emoji) \(group.name)")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        showingAddMembers = true
                    } label: {
                        Label(String(localized: "groupDetail.menu.addMembers"), systemImage: "person.badge.plus")
                    }
                    Button {
                        showingEditGroup = true
                    } label: {
                        Label(String(localized: "groupDetail.menu.editGroup"), systemImage: "pencil")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .overlay {
            if group.contacts.isEmpty {
                ContentUnavailableView(
                    String(localized: "groupDetail.empty.title"),
                    systemImage: "person.badge.plus",
                    description: Text(String(localized: "groupDetail.empty.description"))
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
    @State private var toastMessage = ""
    @State private var showToast = false

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
                    if group.name.count <= 20 {
                        toastMessage = String(localized: "addMembers.toast.addedToNamedGroup \(contact.fullName) \(group.name)")
                    } else {
                        toastMessage = String(localized: "addMembers.toast.addedToGroup \(contact.fullName)")
                    }
                    showToast = true
                    Task {
                        try? await Task.sleep(for: .seconds(2))
                        showToast = false
                    }
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
            .searchable(text: $searchText, prompt: String(localized: "addMembers.search"))
            .navigationTitle(String(localized: "addMembers.navTitle \(group.name)"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.done")) { dismiss() }
                }
            }
            .overlay {
                if nonMembers.isEmpty && searchText.isEmpty {
                    ContentUnavailableView(
                        String(localized: "addMembers.empty.title"),
                        systemImage: "checkmark.circle",
                        description: Text(String(localized: "addMembers.empty.description"))
                    )
                } else if nonMembers.isEmpty {
                    ContentUnavailableView.search
                }
            }
            .overlay(alignment: .bottom) {
                if showToast {
                    Text(verbatim: toastMessage)
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(red: 0.145, green: 0.388, blue: 0.922))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .shadow(radius: 4)
                        .padding(.bottom, 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: showToast)
        }
    }
}
