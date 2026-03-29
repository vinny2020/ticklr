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
            .searchable(text: $searchText, prompt: "Search your network")
            .navigationTitle("Network")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            showingAddContact = true
                        } label: {
                            Label("New Contact", systemImage: "person.badge.plus")
                        }
                        Button {
                            showingImport = true
                        } label: {
                            Label("Get Started", systemImage: "square.and.arrow.down")
                        }
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .overlay {
                if contacts.isEmpty {
                    ContentUnavailableView(
                        "No contacts yet",
                        systemImage: "person.2.slash",
                        description: Text("Tap + to import from your iPhone or LinkedIn")
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
