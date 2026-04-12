import SwiftUI
import SwiftData

struct NetworkListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var contacts: [Contact]

    @State private var searchText = ""
    @State private var showingImport = false
    @State private var showingAddContact = false

    var filtered: [Contact] {
        guard !searchText.isEmpty else { return contacts }
        return contacts.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { contact in
                NavigationLink(destination: ContactDetailView(contact: contact)) {
                    ContactRowView(contact: contact)
                }
            }
            .listStyle(.plain)
            .searchable(text: $searchText, prompt: String(localized: "networkList.search"))
            .navigationTitle(String(localized: "networkList.navTitle"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            showingAddContact = true
                        } label: {
                            Label(String(localized: "networkList.menu.newContact"), systemImage: "person.badge.plus")
                        }
                        Button {
                            showingImport = true
                        } label: {
                            Label(String(localized: "networkList.menu.getStarted"), systemImage: "square.and.arrow.down")
                        }
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .overlay {
                if contacts.isEmpty {
                    ContentUnavailableView(
                        String(localized: "networkList.empty.title"),
                        systemImage: "person.2.slash",
                        description: Text(String(localized: "networkList.empty.description"))
                    )
                } else if !searchText.isEmpty && filtered.isEmpty {
                    ContentUnavailableView.search
                }
            }
            .sheet(isPresented: $showingImport) {
                ImportView()
            }
            .sheet(isPresented: $showingAddContact) {
                AddContactView()
            }
        }
    }
}
