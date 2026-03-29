import SwiftUI
import SwiftData

struct ComposeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]

    @State private var selectedContacts: Set<UUID> = []
    @State private var selectedTemplate: MessageTemplate?
    @State private var messageBody = ""
    @State private var showingComposer = false
    @State private var showingCannotSendAlert = false
    @State private var searchText = ""

    var filteredContacts: [Contact] {
        guard !searchText.isEmpty else { return contacts }
        return contacts.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText) ||
            $0.jobTitle.localizedCaseInsensitiveContains(searchText)
        }
    }

    var recipients: [String] {
        contacts
            .filter { selectedContacts.contains($0.id) }
            .flatMap { $0.phoneNumbers }
    }

    var selectedHaveNoPhones: Bool {
        !selectedContacts.isEmpty && recipients.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Recipients") {
                    ForEach(filteredContacts) { contact in
                        Button {
                            if selectedContacts.contains(contact.id) {
                                selectedContacts.remove(contact.id)
                            } else {
                                selectedContacts.insert(contact.id)
                                if contact.phoneNumbers.isEmpty {
                                    print("Ticklr: ⚠️ \(contact.fullName) has no phone number — cannot include in SMS")
                                }
                            }
                        } label: {
                            HStack {
                                ContactRowView(contact: contact)
                                Spacer()
                                if selectedContacts.contains(contact.id) {
                                    Image(systemName: contact.phoneNumbers.isEmpty
                                          ? "exclamationmark.circle"
                                          : "checkmark.circle.fill")
                                        .foregroundStyle(contact.phoneNumbers.isEmpty ? .orange : .indigo)
                                }
                            }
                        }
                        .tint(.primary)
                    }
                }

                Section("Message") {
                    if !templates.isEmpty {
                        Picker("Template", selection: $selectedTemplate) {
                            Text("None").tag(Optional<MessageTemplate>.none)
                            ForEach(templates) { t in
                                Text(t.title).tag(Optional(t))
                            }
                        }
                        .onChange(of: selectedTemplate) { _, template in
                            messageBody = template?.body ?? ""
                        }
                    }
                    TextEditor(text: $messageBody)
                        .frame(minHeight: 80)
                }

                if selectedHaveNoPhones {
                    Section {
                        Label("Selected contacts have no phone numbers", systemImage: "exclamationmark.triangle")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }
                }
            }
            .navigationTitle("Compose")
            .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search contacts")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        if MessageComposerView.canSendMessages() {
                            showingComposer = true
                        } else {
                            showingCannotSendAlert = true
                        }
                    } label: {
                        HStack(spacing: 5) {
                            if !selectedContacts.isEmpty {
                                Text("\(selectedContacts.count)")
                                    .font(.subheadline.weight(.semibold))
                            }
                            Image(systemName: "paperplane.fill")
                                .font(.subheadline)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(recipients.isEmpty ? Color(.systemGray5) : Color.indigo)
                        .foregroundStyle(recipients.isEmpty ? Color(.systemGray2) : .white)
                        .clipShape(Capsule())
                    }
                    .disabled(recipients.isEmpty)
                }
            }
            .sheet(isPresented: $showingComposer) {
                MessageComposerView(recipients: recipients, body: messageBody)
            }
            .alert("Cannot Send Messages", isPresented: $showingCannotSendAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("This device is not configured to send SMS messages.")
            }
        }
    }
}
