import SwiftUI
import SwiftData

struct ComposeView: View {
    var onCancel: (() -> Void)? = nil
    var initialContact: Contact? = nil

    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]

    @State private var selectedContact: Contact?
    @State private var selectedTemplate: MessageTemplate?
    @State private var messageBody = ""
    @State private var searchText = ""
    @State private var showingComposer = false
    @State private var showingCannotSendAlert = false
    @FocusState private var searchFocused: Bool

    // MARK: — Derived

    var filteredContacts: [Contact] {
        guard !searchText.isEmpty else { return [] }
        return contacts.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText)
        }
    }

    var recipientPhone: String? {
        selectedContact?.phoneNumbers.first
    }

    var canSend: Bool {
        selectedContact != nil && recipientPhone != nil && !messageBody.trimmingCharacters(in: .whitespaces).isEmpty
    }

    // MARK: — Body

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {

                    // MARK: To field
                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "compose.label.to"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal)

                        if let contact = selectedContact {
                            // Selected contact chip
                            HStack {
                                Text(contact.fullName)
                                    .font(.body)
                                    .fontWeight(.medium)
                                Spacer()
                                Button {
                                    selectedContact = nil
                                    searchText = ""
                                    searchFocused = true
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .padding(.horizontal)

                            if contact.phoneNumbers.isEmpty {
                                Label(String(localized: "compose.warning.noPhone"), systemImage: "exclamationmark.triangle.fill")
                                    .font(.caption)
                                    .foregroundStyle(.orange)
                                    .padding(.horizontal)
                            }
                        } else {
                            // Search field
                            TextField(String(localized: "compose.search"), text: $searchText)
                                .textFieldStyle(.roundedBorder)
                                .focused($searchFocused)
                                .padding(.horizontal)
                                .autocorrectionDisabled()

                            // Dropdown results
                            if !filteredContacts.isEmpty {
                                VStack(spacing: 0) {
                                    ForEach(filteredContacts) { contact in
                                        Button {
                                            selectedContact = contact
                                            searchText = ""
                                            searchFocused = false
                                        } label: {
                                            HStack {
                                                VStack(alignment: .leading, spacing: 2) {
                                                    Text(contact.fullName)
                                                        .font(.body)
                                                        .foregroundStyle(.primary)
                                                    if !contact.company.isEmpty {
                                                        Text(contact.company)
                                                            .font(.caption)
                                                            .foregroundStyle(.secondary)
                                                    }
                                                }
                                                Spacer()
                                                if contact.phoneNumbers.isEmpty {
                                                    Image(systemName: "exclamationmark.circle")
                                                        .foregroundStyle(.orange)
                                                        .font(.caption)
                                                }
                                            }
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 10)
                                        }
                                        if contact.id != filteredContacts.last?.id {
                                            Divider().padding(.leading, 14)
                                        }
                                    }
                                }
                                .background(Color(.secondarySystemBackground))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .padding(.horizontal)
                            }
                        }
                    }

                    // MARK: Template dropdown (only if templates exist)
                    if !templates.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(String(localized: "compose.label.template"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)

                            Menu {
                                Button(String(localized: "compose.template.none")) {
                                    selectedTemplate = nil
                                }
                                ForEach(templates) { template in
                                    Button(template.title) {
                                        selectedTemplate = template
                                        messageBody = template.body
                                    }
                                }
                            } label: {
                                HStack {
                                    Text(verbatim: selectedTemplate?.title ?? String(localized: "compose.placeholder.template"))
                                        .foregroundStyle(selectedTemplate == nil ? .secondary : .primary)
                                    Spacer()
                                    Image(systemName: "chevron.up.chevron.down")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(Color(.secondarySystemBackground))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .padding(.horizontal)
                        }
                    }

                    // MARK: Message body
                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "compose.label.message"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal)

                        TextEditor(text: $messageBody)
                            .frame(minHeight: 120)
                            .padding(8)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .padding(.horizontal)
                    }

                    // MARK: Send button
                    HStack {
                        Spacer()
                        Button {
                            if MessageComposerView.canSendMessages() {
                                showingComposer = true
                            } else {
                                showingCannotSendAlert = true
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "paperplane.fill")
                                Text(String(localized: "compose.button.send"))
                                    .fontWeight(.semibold)
                            }
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(canSend ? Color.indigo : Color(.systemGray5))
                            .foregroundStyle(canSend ? .white : Color(.systemGray2))
                            .clipShape(Capsule())
                        }
                        .disabled(!canSend)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 16)
                }
                .padding(.top, 16)
            }
            .onAppear {
                if let c = initialContact, selectedContact == nil {
                    selectedContact = c
                }
            }
            .navigationTitle(String(localized: "compose.navTitle"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    // Always shown so the user can leave the Compose tab and
                    // return to Tickle (the new home) without having to tap
                    // another tab. clearCompose() is a no-op when the form
                    // is already empty.
                    Button(String(localized: "common.cancel")) {
                        clearCompose()
                        onCancel?()
                    }
                    .foregroundStyle(.secondary)
                }
            }
            .sheet(isPresented: $showingComposer, onDismiss: clearCompose) {
                if let phone = recipientPhone {
                    MessageComposerView(recipients: [phone], body: messageBody)
                }
            }
            .alert(String(localized: "compose.alert.cannotSend.title"), isPresented: $showingCannotSendAlert) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(String(localized: "compose.alert.cannotSend.message"))
            }
        }
    }

    // MARK: — Helpers

    private func clearCompose() {
        selectedContact = nil
        selectedTemplate = nil
        messageBody = ""
        searchText = ""
    }
}
