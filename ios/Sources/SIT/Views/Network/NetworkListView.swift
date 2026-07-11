import SwiftUI
import SwiftData

struct NetworkListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]

    @State private var searchText = ""
    @State private var filter: Filter = .all
    @State private var showingImport = false
    @State private var showingAddContact = false
    @State private var selectedContact: Contact?
    /// Import result toast (TIC-85) — set either by the "Get started" sheet's
    /// own completion callback, or picked up in `onAppear` when onboarding just
    /// landed here via `PostOnboardingLanding`.
    @State private var importToastMessage: String?

    // MARK: Row quick actions (TIC-92)
    /// A contact to compose to, paired with the due tickle (if any) that a send
    /// should auto-complete — mirrors the ContactDetail "Send a text" chip
    /// semantics (TIC-82/86) without reaching into ContactDetailView.
    @State private var composeTarget: ComposeTarget?
    /// The contact whose new-tickle editor is presented — set by the row's
    /// "Create a tickle" action and by accepting the post-send suggestion offer.
    @State private var tickleEditContact: Contact?
    /// Non-nil while the "Tickle marked done — Undo" toast is up after a send
    /// from a row's "Send a text" auto-completed a due tickle (TIC-82).
    @State private var completionSnapshot: TickleScheduler.CompletionSnapshot?
    /// Non-nil while a plain save-confirmation toast is up after the row's
    /// "Create a tickle" editor saved (TIC-84).
    @State private var saveToastMessage: String?
    /// Drives the "Create a tickle for [name]?" offer after a plain (reminder-less)
    /// send from a row's "Send a text" (TIC-86).
    @State private var tickleSuggestion: TickleSuggestion?

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    /// A contact to compose to, paired with the due tickle (if any) a send should
    /// auto-complete. Identifiable so it can drive a `.sheet(item:)`.
    private struct ComposeTarget: Identifiable {
        let id = UUID()
        let contact: Contact
        let reminder: TickleReminder?
    }

    /// Filter taxonomy from HANDOFF lines 56-58: All + 4 canonical chips, with
    /// Milestones intentionally excluded (it's a Tickle-tab concept). TIC-88
    /// appends the user's custom groups after the canonical 4, keyed by the
    /// group's stable `id` so the chip's selection survives list re-sorts.
    private enum Filter: Hashable {
        case all
        case category(WarmCategory)
        case userGroup(UUID)
    }

    private static let chipCategories: [WarmCategory] =
        [.family, .friends, .work, .community]

    var body: some View {
        NavigationSplitView {
            ScrollViewReader { proxy in
                List(selection: $selectedContact) {
                    // MARK: Inline title + subtitle (scrolls away)
                    Section {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(String(localized: "networkList.navTitle"))
                                .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                                .tracking(WarmHeadingFont.tracking(warmth: warmth))
                                .foregroundStyle(palette.ink)
                            Text(String(localized: "warm.network.subtitle",
                                        defaultValue: "The people you keep in your circle."))
                                .font(.system(size: 14))
                                .foregroundStyle(palette.ink2)
                        }
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                    }

                    // MARK: Filter chips (scroll with content)
                    Section {
                        filterChipsRow
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 8, trailing: 0))
                    }

                    // MARK: Contacts, sectioned A–Z (TIC-92)
                    ForEach(sections) { section in
                        Section {
                            ForEach(section.items) { contact in
                                contactRow(for: contact)
                            }
                        } header: {
                            Text(section.letter)
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(palette.ink2)
                                .textCase(nil)
                        }
                        .id(section.letter)
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .background(palette.paper.ignoresSafeArea())
                .overlay(alignment: .trailing) {
                    if sections.count > 1 {
                        SectionIndexRail(letters: sections.map(\.letter), palette: palette) { letter in
                            proxy.scrollTo(letter, anchor: .top)
                        }
                        .padding(.trailing, 1)
                    }
                }
            }
            .searchable(text: $searchText,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: String(localized: "networkList.search"))
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            showingAddContact = true
                        } label: {
                            Label(String(localized: "networkList.menu.newContact"),
                                  systemImage: "person.badge.plus")
                        }
                        Button {
                            showingImport = true
                        } label: {
                            Label(String(localized: "networkList.menu.getStarted"),
                                  systemImage: "square.and.arrow.down")
                        }
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(palette.ink)
                    }
                }
            }
            .sheet(isPresented: $showingImport) {
                ImportView(onImportFinished: { importToastMessage = $0 })
            }
            .sheet(isPresented: $showingAddContact) { AddContactView() }
            .sheet(item: $composeTarget) { target in
                ComposeView(onClose: { composeTarget = nil },
                            initialContact: target.contact,
                            dueReminder: target.reminder,
                            onTickleCompleted: { completionSnapshot = $0 },
                            onSuggestTickle: { tickleSuggestion = $0 })
            }
            .sheet(item: $tickleEditContact) { contact in
                TickleEditView(contact: contact, onSaved: { saveToastMessage = $0 })
            }
            .saveConfirmationToast(message: $importToastMessage, warmth: warmth)
            .saveConfirmationToast(message: $saveToastMessage, warmth: warmth)
            .tickleCompletionToast(snapshot: $completionSnapshot, warmth: warmth) {
                if let snapshot = completionSnapshot {
                    TickleScheduler.undoCompletion(snapshot, context: modelContext)
                }
            }
            .suggestTickleToast(suggestion: $tickleSuggestion, warmth: warmth) { suggestion in
                tickleEditContact = fetchContact(suggestion.contactID)
            }
            .onAppear {
                if let toast = PostOnboardingLanding.consumeToast() {
                    importToastMessage = toast
                }
            }
        } detail: {
            if let selectedContact {
                ContactDetailView(contact: selectedContact, onDeleted: { self.selectedContact = nil })
            } else {
                ContentUnavailableView(
                    String(localized: "warm.network.detail.empty.title",
                           defaultValue: "Pick someone to connect with"),
                    systemImage: "person.crop.circle",
                    description: Text(String(localized: "warm.network.detail.empty.description",
                                              defaultValue: "Choose a contact from the list to see their details."))
                )
                .background(palette.paper.ignoresSafeArea())
            }
        }
    }

    // MARK: - Contact row + quick actions (TIC-92)

    /// A single contact row with swipe actions and a matching context menu.
    /// Both surfaces offer the same trio ContactDetail exposes as chips — Send a
    /// text (reminder-attached if one is due), Create a tickle, and Call (phone
    /// only) — reusing this view's own compose-presentation machinery so we never
    /// touch ContactDetailView. Destructive delete intentionally stays on the
    /// detail screen, not the row.
    @ViewBuilder
    private func contactRow(for contact: Contact) -> some View {
        let accent = WarmCategory.resolve(for: contact).palette.accent
        let hasPhone = !contact.phoneNumbers.isEmpty

        ContactRowView(contact: contact, warmth: warmth)
            .tag(contact)
            .listRowBackground(palette.cardBg)
            .listRowSeparatorTint(palette.cardBorder)
            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            .swipeActions(edge: .leading, allowsFullSwipe: true) {
                if hasPhone {
                    Button { startCompose(contact) } label: {
                        Label(String(localized: "warm.contact.sendTickle", defaultValue: "Send a text"),
                              systemImage: "message.fill")
                    }
                    .tint(accent)
                }
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button { tickleEditContact = contact } label: {
                    Label(String(localized: "warm.contact.createTickle", defaultValue: "Create a tickle"),
                          systemImage: "bell.fill")
                }
                .tint(Color(red: 0.145, green: 0.388, blue: 0.922))
                if hasPhone {
                    Button { call(contact) } label: {
                        Label(String(localized: "warm.contact.call", defaultValue: "Call"),
                              systemImage: "phone.fill")
                    }
                    .tint(.green)
                }
            }
            .contextMenu {
                if hasPhone {
                    Button { startCompose(contact) } label: {
                        Label(String(localized: "warm.contact.sendTickle", defaultValue: "Send a text"),
                              systemImage: "message.fill")
                    }
                }
                Button { tickleEditContact = contact } label: {
                    Label(String(localized: "warm.contact.createTickle", defaultValue: "Create a tickle"),
                          systemImage: "bell.fill")
                }
                if hasPhone {
                    Button { call(contact) } label: {
                        Label(String(localized: "warm.contact.call", defaultValue: "Call"),
                              systemImage: "phone.fill")
                    }
                }
            }
    }

    /// Presents Compose for `contact`, threading in the contact's currently-due
    /// tickle (if any) so a send auto-completes it — matching ContactDetail's
    /// "Send a text" chip (TIC-82). `nil` reminder ⇒ a send completes nothing and
    /// instead offers to create one (TIC-86).
    private func startCompose(_ contact: Contact) {
        let due = contact.tickles
            .filter { TickleScheduler.isDueForSendCompletion($0) }
            .min { $0.nextDueDate < $1.nextDueDate }
        composeTarget = ComposeTarget(contact: contact, reminder: due)
    }

    /// Opens the system dialer for the contact's first phone number.
    private func call(_ contact: Contact) {
        guard let phone = contact.phoneNumbers.first else { return }
        let cleaned = phone.filter { $0.isNumber || $0 == "+" }
        guard !cleaned.isEmpty, let url = URL(string: "tel:\(cleaned)") else { return }
        UIApplication.shared.open(url)
    }

    private func fetchContact(_ id: UUID) -> Contact? {
        let descriptor = FetchDescriptor<Contact>(predicate: #Predicate { $0.id == id })
        return try? modelContext.fetch(descriptor).first
    }

    // MARK: - Filter chip row

    private var filterChipsRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                WarmFilterChip(
                    kind: .all,
                    label: String(localized: "warm.network.filter.all", defaultValue: "All"),
                    count: contacts.count,
                    isActive: filter == .all,
                    action: { filter = .all },
                    warmth: warmth
                )
                ForEach(Self.chipCategories, id: \.self) { cat in
                    let n = countFor(category: cat)
                    if n > 0 || filter == .category(cat) {
                        WarmFilterChip(
                            kind: .category(cat),
                            label: cat.localizedLabel,
                            count: n,
                            isActive: filter == .category(cat),
                            action: { filter = .category(cat) },
                            warmth: warmth
                        )
                    }
                }
                // TIC-88: the user's custom groups follow the canonical chips,
                // horizontally scrollable. Selecting one filters the list to
                // that group's members.
                ForEach(userGroups, id: \.persistentModelID) { group in
                    WarmFilterChip(
                        kind: .userGroup(emoji: group.emoji),
                        label: group.displayName,
                        count: group.contacts.count,
                        isActive: filter == .userGroup(group.id),
                        action: { filter = .userGroup(group.id) },
                        warmth: warmth
                    )
                }
            }
            .padding(.horizontal, 16)
        }
    }

    /// User-created groups only — the 5 canonical category groups are already
    /// represented by their own chips (4 of them) and are excluded here.
    /// Sorted by creation order to match the Groups tab's "Your Groups" list.
    private var userGroups: [ContactGroup] {
        let canonicalIds = Set(WarmCategory.allCases.map { $0.groupUUID })
        return allGroups
            .filter { !canonicalIds.contains($0.id) }
            .sorted { $0.createdAt < $1.createdAt }
    }

    // MARK: - Filtering + sectioning

    private var filtered: [Contact] {
        var result = contacts

        switch filter {
        case .all:
            break
        case .category(let cat):
            result = GroupMembershipFilter.contacts(result, inGroupWithID: cat.groupUUID)
        case .userGroup(let id):
            result = GroupMembershipFilter.contacts(result, inGroupWithID: id)
        }

        if !searchText.isEmpty {
            result = result.filter { ContactSearchFilter.matches($0, query: searchText) }
        }

        return result
    }

    /// A–Z sections over the active filter + search result (TIC-92). Derived once
    /// per body eval; the `List` renders sections lazily, so only visible rows
    /// materialize even across ~2k contacts.
    private var sections: [ContactSectionIndex.Section<Contact>] {
        ContactSectionIndex.sections(from: filtered)
    }

    private func countFor(category: WarmCategory) -> Int {
        let canonicalId = category.groupUUID
        return contacts.reduce(0) { acc, c in
            acc + (c.groups.contains { $0.id == canonicalId } ? 1 : 0)
        }
    }
}

// MARK: - A–Z fast-scroll rail (TIC-92)

/// Compact letter rail overlaid on the trailing edge of the contacts list, the
/// house-reasonable stand-in for UIKit's `sectionIndexTitles`. A tap or vertical
/// drag maps the touch position to a section letter and scrolls the hosting
/// `ScrollViewReader` there; a light selection haptic fires as the active letter
/// changes during a scrub.
private struct SectionIndexRail: View {
    let letters: [String]
    let palette: WarmPalette
    let onSelect: (String) -> Void

    @State private var current: String?

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                ForEach(letters, id: \.self) { letter in
                    Text(letter)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(palette.ink2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        guard !letters.isEmpty, geo.size.height > 0 else { return }
                        let ratio = value.location.y / geo.size.height
                        let idx = min(letters.count - 1, max(0, Int(ratio * CGFloat(letters.count))))
                        let letter = letters[idx]
                        if letter != current {
                            current = letter
                            onSelect(letter)
                            UISelectionFeedbackGenerator().selectionChanged()
                        }
                    }
                    .onEnded { _ in current = nil }
            )
        }
        .frame(width: 16)
    }
}

/// Pure membership predicate behind the Network group filters (TIC-88).
/// Extracted from `NetworkListView.filtered` so both the canonical-category
/// and user-group filter paths share one definition of "is this contact in
/// the group with this id", and so the logic is unit-testable without hosting
/// the SwiftUI view.
enum GroupMembershipFilter {
    static func contacts(_ contacts: [Contact], inGroupWithID id: UUID) -> [Contact] {
        contacts.filter { contact in
            contact.groups.contains { $0.id == id }
        }
    }
}
