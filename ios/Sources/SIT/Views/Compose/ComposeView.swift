import SwiftUI
import SwiftData
import MessageUI

struct ComposeView: View {
    /// Called when the user is finished here — either they confirmed a
    /// discard/cleared an empty draft via Cancel, or the message was sent.
    /// The Compose TAB (ContentView) passes `nil` (TIC-93: staying on
    /// Compose, form reset, is the whole point — there's nowhere to route
    /// back to); sheet presenters (ContactDetailView, notification-tap
    /// compose) still pass a closure that dismisses the sheet.
    var onClose: (() -> Void)? = nil
    var initialContact: Contact? = nil
    /// The currently-due tickle to auto-complete when this message is sent
    /// (TIC-82). Threaded in from the Tickle tab (row/action-sheet/iPad pane)
    /// and from ContactDetail when the contact has a due reminder. `nil` when
    /// Compose is reached directly from its tab — then a send completes nothing.
    var dueReminder: TickleReminder? = nil
    /// Called after a send auto-completes `dueReminder`, handing the parent the
    /// pre-completion snapshot so it can present the "Tickle marked done — Undo"
    /// toast on the screen that remains once this compose surface is dismissed.
    var onTickleCompleted: ((TickleScheduler.CompletionSnapshot) -> Void)? = nil
    /// Called after a plain send (one with no `dueReminder` attached) to offer
    /// creating a tickle for the just-texted contact (TIC-86). Handed a
    /// value-only `TickleSuggestion` so the presenter can host the offer toast
    /// on the screen that remains after this compose surface dismisses. Mutually
    /// exclusive with `onTickleCompleted` for a single send — never both.
    var onSuggestTickle: ((TickleSuggestion) -> Void)? = nil

    @Environment(\.modelContext) private var modelContext
    @Environment(\.horizontalSizeClass) private var hSize
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @Query(sort: \MessageTemplate.title) private var templates: [MessageTemplate]

    @State private var selectedContact: Contact?
    @State private var selectedTemplate: MessageTemplate?
    @State private var messageBody = ""

    @State private var searchText = ""
    @State private var showingBrowseAll = false
    @State private var showingComposer = false
    @State private var showingCannotSendAlert = false
    @State private var showingSendFailedAlert = false
    /// Presents `TemplateListView` as a sheet so templates can be created/edited
    /// without leaving Compose (TIC-90) — reached from both the "Manage
    /// templates…" entry (templates exist) and "Create a template…" (none yet).
    @State private var showingManageTemplates = false
    /// The template awaiting confirmation before it overwrites a hand-typed
    /// draft (TIC-90 draft protection).
    @State private var pendingTemplate: MessageTemplate?
    @State private var showingReplaceDraftConfirm = false
    /// Drives the "Discard draft?" confirmation queued by Cancel when the
    /// message body isn't empty (TIC-93 draft protection).
    @State private var showingDiscardDraftConfirm = false
    /// Outcome reported by the Messages composer; consumed in its onDismiss.
    @State private var composeResult: MessageComposeResult?
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

    /// Recipient suggestions shown while the To-field is empty (TIC-86):
    /// due-today contacts (most overdue first) + recents (excluding due-today).
    var suggestions: (dueToday: [Contact], recents: [Contact]) {
        ComposeSuggestions.assemble(from: contacts)
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

                            if searchText.isEmpty {
                                suggestionsView
                            } else if !filteredContacts.isEmpty {
                                recipientCard(contacts: filteredContacts)
                                    .padding(.horizontal)
                            }
                        }
                    }

                    // Always shown — even with zero templates — so there's a path
                    // from Compose into template management either way (TIC-90).
                    VStack(alignment: .leading, spacing: 6) {
                        WarmEyebrow(text: String(localized: "compose.label.template"), warmth: warmth)
                            .padding(.horizontal)

                        Menu {
                            if templates.isEmpty {
                                Button(String(localized: "compose.template.create")) {
                                    showingManageTemplates = true
                                }
                            } else {
                                Button(String(localized: "compose.template.none")) {
                                    selectedTemplate = nil
                                }
                                ForEach(templates) { template in
                                    Button(template.title) {
                                        selectTemplate(template)
                                    }
                                }
                                Divider()
                                Button(String(localized: "compose.template.manage")) {
                                    showingManageTemplates = true
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
                    // TIC-93: Cancel no longer switches tabs on its own — that's
                    // entirely up to what the caller wires into `onClose` (the
                    // Compose tab passes nil so it stays put; sheet presenters
                    // still dismiss). A non-empty draft is protected behind a
                    // confirmation instead of being silently discarded.
                    Button(String(localized: "common.cancel")) {
                        switch Self.cancelDecision(messageBody: messageBody) {
                        case .clearSilently:
                            clearCompose()
                            onClose?()
                        case .confirmDiscard:
                            showingDiscardDraftConfirm = true
                        }
                    }
                    .foregroundStyle(palette.ink2)
                }
            }
            .sheet(isPresented: $showingComposer, onDismiss: handleComposerDismiss) {
                if let phone = recipientPhone {
                    MessageComposerView(recipients: [phone], body: messageBody) { result in
                        composeResult = result
                    }
                }
            }
            .sheet(isPresented: $showingBrowseAll) {
                ComposeContactPickerSheet(warmth: warmth) { contact in
                    selectedContact = contact
                    searchText = ""
                    searchFocused = false
                }
            }
            // TIC-90: "Manage templates…" / "Create a template…" open full
            // add/edit/delete right from Compose. On dismiss, the live @Query
            // already reflects any edits; if the applied template was deleted
            // while managing, drop the now-dangling selection.
            .sheet(isPresented: $showingManageTemplates, onDismiss: {
                if let applied = selectedTemplate, !templates.contains(where: { $0.id == applied.id }) {
                    selectedTemplate = nil
                }
            }) {
                NavigationStack {
                    TemplateListView()
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button(String(localized: "common.done")) {
                                    showingManageTemplates = false
                                }
                            }
                        }
                }
            }
            // TIC-90 draft protection: only prompts when picking a template
            // would silently clobber hand-typed text — see
            // `shouldConfirmTemplateReplace`.
            .confirmationDialog(
                String(localized: "compose.template.replaceDraft.title"),
                isPresented: $showingReplaceDraftConfirm,
                titleVisibility: .visible,
                presenting: pendingTemplate
            ) { template in
                Button(String(localized: "compose.template.replaceDraft.confirm"), role: .destructive) {
                    applyTemplate(template)
                }
                Button(String(localized: "common.cancel"), role: .cancel) {
                    pendingTemplate = nil
                }
            } message: { _ in
                Text(String(localized: "compose.template.replaceDraft.message"))
            }
            // TIC-93 draft protection: Cancel with a non-empty message body
            // queues this instead of silently clearing the form.
            .confirmationDialog(
                String(localized: "compose.discardDraft.title"),
                isPresented: $showingDiscardDraftConfirm,
                titleVisibility: .visible
            ) {
                Button(String(localized: "compose.discardDraft.confirm"), role: .destructive) {
                    clearCompose()
                    onClose?()
                }
                Button(String(localized: "compose.discardDraft.keepEditing"), role: .cancel) {}
            } message: {
                Text(String(localized: "compose.discardDraft.message"))
            }
            .alert(String(localized: "compose.alert.cannotSend.title"), isPresented: $showingCannotSendAlert) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(String(localized: "compose.alert.cannotSend.message"))
            }
            .alert(String(localized: "compose.alert.sendFailed.title",
                          defaultValue: "Message Not Sent"), isPresented: $showingSendFailedAlert) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(String(localized: "compose.alert.sendFailed.message",
                            defaultValue: "Something went wrong. Your message is still here — try sending it again."))
            }
        }
    }

    // MARK: — Recipient suggestions (TIC-86)

    /// Suggestions shown while the To-field is empty: a "Due today" section
    /// (most overdue first), a capped "Recents" section, and an always-present
    /// "All contacts…" browse affordance so the user can pick without typing.
    @ViewBuilder
    private var suggestionsView: some View {
        let sections = suggestions
        VStack(alignment: .leading, spacing: 14) {
            if !sections.dueToday.isEmpty {
                suggestionSection(
                    title: String(localized: "compose.suggest.section.dueToday"),
                    contacts: sections.dueToday
                )
            }
            if !sections.recents.isEmpty {
                suggestionSection(
                    title: String(localized: "compose.suggest.section.recents"),
                    contacts: sections.recents
                )
            }
            browseAllButton
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private func suggestionSection(title: String, contacts: [Contact]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            WarmEyebrow(text: title, warmth: warmth)
                .padding(.leading, 2)
            recipientCard(contacts: contacts)
        }
    }

    /// Card of tappable recipient rows — shared by the filtered search results
    /// and the empty-field suggestion sections so they read identically.
    @ViewBuilder
    private func recipientCard(contacts: [Contact]) -> some View {
        VStack(spacing: 0) {
            ForEach(contacts) { contact in
                recipientRow(contact: contact)
                if contact.id != contacts.last?.id {
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
    }

    private func recipientRow(contact: Contact) -> some View {
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
                // Contacts without a phone number stay visible but flagged, so
                // the "no phone" state is surfaced at selection time as before.
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
    }

    private var browseAllButton: some View {
        Button {
            searchFocused = false
            showingBrowseAll = true
        } label: {
            HStack {
                Text(String(localized: "compose.suggest.browseAll"))
                    .font(.body)
                    .foregroundStyle(palette.ink)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(palette.ink3)
                    .flipsForRightToLeftLayoutDirection(true)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(palette.cardBg)
            .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                    .stroke(palette.cardBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: — Helpers

    /// Routes by composer outcome: sent → stamp the contact, reset, and call
    /// `onClose` (TIC-93: on the Compose tab that's `nil`, so the reset form
    /// just stays put; sheet presenters still flow back via their closure);
    /// cancelled/failed → keep the draft so the user can tweak and retry.
    private func handleComposerDismiss() {
        let result = composeResult
        composeResult = nil

        // Only a confirmed send stamps the contact and auto-completes the tickle;
        // cancelled/failed leave both untouched (TIC-82).
        guard Self.isSuccessfulSend(result) else {
            if result == .failed { showingSendFailedAlert = true }
            return
        }

        selectedContact?.lastContactedAt = .now
        try? modelContext.save()

        // At most one post-send surface (TIC-82 undo ↔ TIC-86 offer exclusivity):
        // a reminder-attached send routes to completion/undo; a plain send offers
        // a tickle only when the contact has no other open (active/snoozed)
        // reminder — someone already covered mustn't be offered a duplicate.
        let surface = ComposeSuggestions.postSendSurface(
            result: result,
            hadDueReminderAttached: dueReminder != nil,
            contactHasOpenReminder: selectedContact.map(ComposeSuggestions.hasOpenReminder) ?? false
        )

        switch surface {
        case .completionUndo:
            // Auto-complete the associated due tickle. Validates against the live
            // store first, so a reminder deleted/completed while the composer was
            // up won't be double-completed. The snapshot is handed to the parent
            // to drive the undo toast on the screen that remains after dismissal.
            let snapshot = dueReminder.flatMap {
                TickleScheduler.completeAfterSend(reminder: $0, context: modelContext)
            }
            clearCompose()
            onClose?()
            if let snapshot { onTickleCompleted?(snapshot) }

        case .suggestTickle:
            // Plain send to an uncovered contact: capture a value-only handle to
            // them BEFORE clearing the form, then offer to create a tickle on the
            // surface that remains.
            let suggestion = selectedContact.map {
                TickleSuggestion(contactID: $0.id, contactName: $0.fullName)
            }
            clearCompose()
            onClose?()
            if let suggestion { onSuggestTickle?(suggestion) }

        case .none:
            // Confirmed send, but no surface to show (contact already covered by
            // an open reminder). Still reset and flow back like any other send.
            clearCompose()
            onClose?()
        }
    }

    /// Only a confirmed `.sent` result stamps the contact and completes the due
    /// tickle; `.cancelled` / `.failed` (and `nil`) do neither (TIC-82).
    static func isSuccessfulSend(_ result: MessageComposeResult?) -> Bool {
        result == .sent
    }

    private func clearCompose() {
        selectedContact = nil
        selectedTemplate = nil
        messageBody = ""
        searchText = ""
        pendingTemplate = nil
    }

    // MARK: — Template selection + draft protection (TIC-90)

    /// Picking a template from the menu: applies immediately unless doing so
    /// would silently clobber hand-typed text, in which case a confirmation is
    /// queued instead.
    private func selectTemplate(_ template: MessageTemplate) {
        switch Self.templateReplaceDecision(currentBody: messageBody, appliedTemplateBody: selectedTemplate?.body) {
        case .applyDirectly:
            applyTemplate(template)
        case .confirmReplace:
            pendingTemplate = template
            showingReplaceDraftConfirm = true
        }
    }

    private func applyTemplate(_ template: MessageTemplate) {
        selectedTemplate = template
        messageBody = template.body
        pendingTemplate = nil
    }

    enum TemplateReplaceDecision: Equatable {
        case applyDirectly
        case confirmReplace
    }

    // MARK: - Cancel + draft protection (TIC-93)

    enum CancelDecision: Equatable {
        case clearSilently
        case confirmDiscard
    }

    /// Pure decision for whether tapping Cancel needs to confirm before
    /// discarding the draft (TIC-93). A trimmed-empty body has nothing to
    /// protect and clears/stays silently; any hand-typed text queues the
    /// "Discard draft?" confirmation instead of being silently dropped.
    static func cancelDecision(messageBody: String) -> CancelDecision {
        messageBody.trimmingCharacters(in: .whitespaces).isEmpty ? .clearSilently : .confirmDiscard
    }

    /// Pure decision for whether picking a new template needs to confirm
    /// before overwriting the current draft (TIC-90). Applies directly when
    /// the body is empty, or still equals the body of the template currently
    /// applied (i.e. untouched since it was picked) — only text the user
    /// actually typed themselves needs protecting.
    static func templateReplaceDecision(currentBody: String, appliedTemplateBody: String?) -> TemplateReplaceDecision {
        guard !currentBody.trimmingCharacters(in: .whitespaces).isEmpty else { return .applyDirectly }
        if let appliedTemplateBody, currentBody == appliedTemplateBody { return .applyDirectly }
        return .confirmReplace
    }
}

// MARK: - Browse-all recipient picker (TIC-86)

/// Full alphabetical contact picker reached from the "All contacts…" affordance,
/// letting the user pick a recipient without typing. Reuses the shared
/// `contactPicker.*` strings. Contacts without a phone number stay visible but
/// flagged, matching the inline compose list.
private struct ComposeContactPickerSheet: View {
    var warmth: Warmth = .subtle
    let onSelect: (Contact) -> Void

    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @State private var searchText = ""

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var filtered: [Contact] {
        guard !searchText.isEmpty else { return contacts }
        return contacts.filter {
            $0.fullName.localizedCaseInsensitiveContains(searchText) ||
            $0.company.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { contact in
                Button {
                    onSelect(contact)
                    dismiss()
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
                }
                .listRowBackground(palette.cardBg)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .searchable(text: $searchText, prompt: String(localized: "contactPicker.search"))
            .navigationTitle(String(localized: "contactPicker.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
            }
            .overlay {
                if contacts.isEmpty {
                    ContentUnavailableView(
                        String(localized: "contactPicker.empty.title"),
                        systemImage: "person.slash",
                        description: Text(String(localized: "contactPicker.empty.description"))
                    )
                } else if !searchText.isEmpty && filtered.isEmpty {
                    ContentUnavailableView.search
                }
            }
        }
    }
}
