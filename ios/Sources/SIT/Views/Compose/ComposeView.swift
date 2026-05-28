import SwiftUI
import SwiftData

struct ComposeView: View {
    var onCancel: (() -> Void)? = nil
    var initialContact: Contact? = nil

    @Environment(\.modelContext) private var modelContext
    @Environment(\.horizontalSizeClass) private var hSize
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]

    @State private var selectedContact: Contact?
    @State private var selectedTemplate: MessageTemplate?
    @State private var messageBody = ""

    @State private var searchText = ""
    @State private var showingComposer = false
    @State private var showingCannotSendAlert = false
    @FocusState private var searchFocused: Bool

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }
    /// Send button accent follows the selected contact's resolved
    /// category when one is picked; otherwise falls back to community.
    private var accent: Color {
        if let c = selectedContact {
            return WarmCategory.resolve(for: c).palette.accent
        }
        return WarmCategory.community.palette.accent
    }

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

                    // MARK: Inline warm title (matches Groups / Tickle / Network)
                    Text(String(localized: "compose.navTitle"))
                        .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                        .tracking(WarmHeadingFont.tracking(warmth: warmth))
                        .foregroundStyle(palette.ink)
                        .padding(.horizontal)
                        .padding(.top, 8)

                    // MARK: To field
                    VStack(alignment: .leading, spacing: 6) {
                        WarmEyebrow(text: String(localized: "compose.label.to"), warmth: warmth)
                            .padding(.horizontal)

                        if let contact = selectedContact {
                            HStack {
                                Text(contact.fullName)
                                    .font(.body)
                                    .fontWeight(.medium)
                                    .foregroundStyle(palette.ink)
                                Spacer()
                                Button {
                                    selectedContact = nil
                                    searchText = ""
                                    searchFocused = true
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundStyle(palette.ink2)
                                }
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(palette.cardBg)
                            .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                                    .stroke(palette.cardBorder, lineWidth: 1)
                            )
                            .padding(.horizontal)

                            if contact.phoneNumbers.isEmpty {
                                Label(String(localized: "compose.warning.noPhone"), systemImage: "exclamationmark.triangle.fill")
                                    .font(.caption)
                                    .foregroundStyle(.orange)
                                    .padding(.horizontal)
                            }
                        } else {
                            TextField(String(localized: "compose.search"), text: $searchText)
                                .textFieldStyle(.plain)
                                .focused($searchFocused)
                                .autocorrectionDisabled()
                                .padding(.horizontal, 12)
                                .padding(.vertical, 10)
                                .background(palette.cardBg)
                                .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                                        .stroke(palette.cardBorder, lineWidth: 1)
                                )
                                .padding(.horizontal)

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
                                                        .foregroundStyle(palette.ink)
                                                    if !contact.company.isEmpty {
                                                        Text(contact.company)
                                                            .font(.caption)
                                                            .foregroundStyle(palette.ink2)
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
                                        .buttonStyle(.plain)
                                        if contact.id != filteredContacts.last?.id {
                                            Rectangle()
                                                .fill(palette.cardBorder)
                                                .frame(height: 1)
                                                .padding(.leading, 14)
                                        }
                                    }
                                }
                                .background(palette.cardBg)
                                .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                                        .stroke(palette.cardBorder, lineWidth: 1)
                                )
                                .padding(.horizontal)
                            }
                        }
                    }

                    if !templates.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            WarmEyebrow(text: String(localized: "compose.label.template"), warmth: warmth)
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
                                        .foregroundStyle(selectedTemplate == nil ? palette.ink2 : palette.ink)
                                    Spacer()
                                    Image(systemName: "chevron.up.chevron.down")
                                        .font(.caption)
                                        .foregroundStyle(palette.ink2)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(palette.cardBg)
                                .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                                        .stroke(palette.cardBorder, lineWidth: 1)
                                )
                            }
                            .padding(.horizontal)
                        }
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        WarmEyebrow(text: String(localized: "compose.label.message"), warmth: warmth)
                            .padding(.horizontal)

                        TextEditor(text: $messageBody)
                            .frame(minHeight: 120)
                            .padding(8)
                            .scrollContentBackground(.hidden)
                            .background(palette.cardBg)
                            .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                                    .stroke(palette.cardBorder, lineWidth: 1)
                            )
                            .padding(.horizontal)
                    }

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
                                    .flipsForRightToLeftLayoutDirection(true)
                                Text(String(localized: "compose.button.send"))
                                    .fontWeight(.semibold)
                            }
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(canSend ? accent : palette.paperSurfaceAlt)
                            .foregroundStyle(canSend ? Color(red: 0.98, green: 0.96, blue: 0.89) : palette.ink3)
                            .clipShape(Capsule())
                        }
                        .disabled(!canSend)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 16)
                }
                .padding(.top, 16)
            }
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .contentMargins(.horizontal, hSize == .regular ? 196 : 0, for: .scrollContent)
            .onAppear {
                if let c = initialContact, selectedContact == nil {
                    selectedContact = c
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
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
                    .foregroundStyle(palette.ink2)
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
