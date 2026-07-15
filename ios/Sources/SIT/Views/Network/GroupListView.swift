import SwiftUI
import SwiftData

struct GroupListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ContactGroup.name) private var groups: [ContactGroup]

    @State private var showingAddGroup = false
    @State private var editingGroup: ContactGroup?
    @State private var selectedGroup: ContactGroup?
    /// The user group awaiting delete confirmation. Deleting a group cascades to
    /// every tickle attached to it (TickleScheduler.deleteGroup), so — unlike the
    /// undoable mark-done/snooze — it's guarded by a destructive confirmation that
    /// spells out the cascade count (TIC-87).
    @State private var groupPendingDeletion: ContactGroup?
    /// Drives the create-with-members flow (TIC-88): creating a group parks it
    /// here, and the create sheet's dismissal promotes it to the Add Members
    /// sheet. See `GroupCreationFlow`.
    @State private var creationFlow = GroupCreationFlow()

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    var body: some View {
        // A NavigationStack (not NavigationSplitView) drives group navigation.
        // The sidebar is a custom warm ScrollView of WarmCards rather than a
        // `List(selection:)`, so a split view has nothing to observe and never
        // pushes the detail column on compact (iPhone) widths — tapping a group
        // silently did nothing. `navigationDestination(item:)` pushes correctly
        // on both iPhone and iPad while preserving the warm sidebar layout.
        NavigationStack {
            sidebar
                .navigationDestination(item: $selectedGroup) { group in
                    GroupDetailView(group: group)
                }
        }
    }

    // MARK: - Columns

    private var scrollContent: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            header
                .padding(.horizontal, WarmSpacing.lg)
                .padding(.top, 8)

            ForEach(canonicalGroups, id: \.persistentModelID) { group in
                canonicalRow(group)
                    .padding(.horizontal, WarmSpacing.lg)
            }

            if !userGroups.isEmpty {
                WarmEyebrow(text: yourGroupsLabel, warmth: warmth)
                    .padding(.horizontal, WarmSpacing.lg)
                    .padding(.top, 12)
                WarmListContainer(warmth: warmth) {
                    ForEach(Array(userGroups.enumerated()), id: \.element.persistentModelID) { idx, group in
                        userGroupRow(group)
                        if idx < userGroups.count - 1 {
                            WarmRowDivider(warmth: warmth)
                        }
                    }
                }
                .padding(.horizontal, WarmSpacing.lg)
            }
        }
        .padding(.bottom, 24)
    }

    @ViewBuilder
    private var sidebar: some View {
        ScrollView {
                scrollContent
            }
            .background(palette.paper.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showingAddGroup = true
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(palette.ink)
                    }
                }
            }
            .sheet(isPresented: $showingAddGroup, onDismiss: { creationFlow.createSheetDismissed() }) {
                GroupEditSheet(group: nil, onCreated: { creationFlow.groupCreated($0) })
            }
            .sheet(item: $editingGroup) { group in
                GroupEditSheet(group: group)
            }
            // Presented only once the create sheet has fully dismissed —
            // stacking two sheets in the same frame drops one (TIC-88).
            .sheet(item: populateGroupBinding) { group in
                AddMembersSheet(group: group)
            }
            .confirmationDialog(
                String(localized: "groupList.deleteConfirm.title \(groupPendingDeletion?.displayName ?? "")"),
                isPresented: Binding(
                    get: { groupPendingDeletion != nil },
                    set: { if !$0 { groupPendingDeletion = nil } }
                ),
                titleVisibility: .visible,
                presenting: groupPendingDeletion
            ) { group in
                Button(String(localized: "common.delete"), role: .destructive) {
                    if selectedGroup == group { selectedGroup = nil }
                    TickleScheduler.deleteGroup(group, context: modelContext)
                    try? modelContext.save()
                }
            } message: { group in
                Text(String(localized: "groupList.deleteConfirm.message \(group.tickles.count)"))
            }
            .overlay {
                if groups.isEmpty {
                    ContentUnavailableView(
                        String(localized: "groupList.empty.title"),
                        systemImage: "person.3.sequence",
                        description: Text(String(localized: "groupList.empty.description"))
                    )
                }
            }
    }

    // MARK: - Pieces

    /// Drives the deferred Add Members sheet for a just-created group. Extracted
    /// from the view builder so the type-checker doesn't choke on an inline
    /// `Binding(get:set:)` inside the already-large body.
    private var populateGroupBinding: Binding<ContactGroup?> {
        Binding(
            get: { creationFlow.groupToPopulate },
            set: { if $0 == nil { creationFlow.populateFinished() } }
        )
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(String(localized: "groupList.navTitle"))
                .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                .tracking(WarmHeadingFont.tracking(warmth: warmth))
                .foregroundStyle(palette.ink)
            Text(String(localized: "warm.groups.subtitle",
                        defaultValue: "Circles that shape your world."))
                .font(.system(size: 14))
                .foregroundStyle(palette.ink2)
        }
    }

    @ViewBuilder
    private func canonicalRow(_ group: ContactGroup) -> some View {
        if let category = WarmCategory.from(groupId: group.id) {
            WarmCard(
                category: category,
                variant: .row,
                warmth: warmth,
                showPrompt: true,
                contactsCount: group.contacts.count,
                onTap: { selectedGroup = group }
            )
            // Stable, locale-independent hook so UI tests can tap a specific
            // canonical row and assert it navigates (regression guard for the
            // tap-doesn't-open bug fixed alongside this).
            .accessibilityIdentifier("groupRow.canonical.\(category.rawValue)")
            .contextMenu {
                Button(String(localized: "common.edit"), systemImage: "pencil") {
                    editingGroup = group
                }
                // Canonical groups intentionally not deletable —
                // CanonicalGroupSeed would re-add them on next launch.
            }
        }
    }

    private func userGroupRow(_ group: ContactGroup) -> some View {
        Button {
            selectedGroup = group
        } label: {
            HStack(spacing: 14) {
                Text(group.emoji)
                    .font(.system(size: 28))
                    .frame(width: 64, height: 64)
                    .background(palette.paperSurface)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 4) {
                    Text(group.displayName)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(palette.ink)
                        .lineLimit(1)
                    Text(String(localized: "groupList.contactCount \(group.contacts.count)"))
                        .font(.system(size: 13))
                        .foregroundStyle(palette.ink2)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(palette.ink3)
                    .flipsForRightToLeftLayoutDirection(true)
            }
            .padding(.horizontal, WarmSpacing.lg)
            .padding(.vertical, 16)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("groupRow.user.\(group.id.uuidString)")
        .contextMenu {
            Button(String(localized: "common.edit"), systemImage: "pencil") {
                editingGroup = group
            }
            Button(String(localized: "common.delete"), systemImage: "trash", role: .destructive) {
                groupPendingDeletion = group
            }
        }
    }

    // MARK: - Sorting

    private var canonicalGroups: [ContactGroup] {
        let canonicalIds = Set(WarmCategory.allCases.map { $0.groupUUID })
        let canonical = groups.filter { canonicalIds.contains($0.id) }
        return canonical.sorted { lhs, rhs in
            let l = WarmCategory.from(groupId: lhs.id)?.sortOrder ?? Int.max
            let r = WarmCategory.from(groupId: rhs.id)?.sortOrder ?? Int.max
            return l < r
        }
    }

    private var userGroups: [ContactGroup] {
        let canonicalIds = Set(WarmCategory.allCases.map { $0.groupUUID })
        return groups
            .filter { !canonicalIds.contains($0.id) }
            .sorted { $0.createdAt < $1.createdAt }
    }

    private var yourGroupsLabel: String {
        String(localized: "warm.groups.yourGroups", defaultValue: "Your Groups")
    }
}

// MARK: - Create-with-members flow (TIC-88)

/// Two-phase "create a group, then immediately populate it" state machine.
///
/// A freshly created group can't be handed to the Add Members sheet while the
/// create sheet is still on screen — presenting a second sheet mid-dismiss
/// races and one gets dropped. So creation parks the new group in
/// `pendingCreatedGroup`; the create sheet's `onDismiss` promotes it to
/// `groupToPopulate`, which drives the Add Members sheet. A cancelled create
/// (nothing parked) is a no-op. Extracted as a value type so the transition
/// rules are unit-testable without hosting the view.
struct GroupCreationFlow {
    private(set) var pendingCreatedGroup: ContactGroup?
    var groupToPopulate: ContactGroup?

    /// A group was just created in the edit sheet — park it until the sheet closes.
    mutating func groupCreated(_ group: ContactGroup) {
        pendingCreatedGroup = group
    }

    /// The create/edit sheet finished dismissing. Promote a freshly created
    /// group into the populate slot; a plain cancel (nothing parked) does nothing.
    mutating func createSheetDismissed() {
        guard let group = pendingCreatedGroup else { return }
        pendingCreatedGroup = nil
        groupToPopulate = group
    }

    /// The Add Members sheet closed — clear the populate slot.
    mutating func populateFinished() {
        groupToPopulate = nil
    }
}

// MARK: - Create / Edit Sheet

struct GroupEditSheet: View {
    let group: ContactGroup?
    /// Called with the newly-inserted group right after a successful *create*
    /// (never on edit), so the presenter can immediately open Add Members for
    /// it — the create-with-members flow (TIC-88). Fires after the context is
    /// saved so the group is fully persisted.
    var onCreated: ((ContactGroup) -> Void)? = nil
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]

    @State private var name: String
    @State private var emoji: String

    private let suggestedEmoji = [
        "👥", "⭐️", "💼", "🏠", "🎓", "❤️",
        "🌍", "🏋️", "🎵", "📚", "🍕", "✈️",
        "🎯", "💡", "🌱", "🔥", "🎉", "🤝"
    ]

    init(group: ContactGroup?, onCreated: ((ContactGroup) -> Void)? = nil) {
        self.group = group
        self.onCreated = onCreated
        _name = State(initialValue: group?.displayName ?? "")
        _emoji = State(initialValue: group?.emoji ?? "👥")
    }

    private var isEditing: Bool { group != nil }
    /// Canonical category groups derive their name from the device language,
    /// so the name field is read-only — only the emoji is editable.
    private var isCanonical: Bool { group?.isCanonicalCategory ?? false }
    private var isDuplicate: Bool {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return false }
        return allGroups.contains {
            $0.name.caseInsensitiveCompare(trimmed) == .orderedSame &&
            $0.id != group?.id
        }
    }
    private var canSave: Bool {
        if isCanonical { return true }   // only emoji is editable; always valid
        return !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        name.count <= 30 &&
        !isDuplicate
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "groupEdit.section.emoji")) {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 12) {
                        ForEach(suggestedEmoji, id: \.self) { e in
                            Button {
                                emoji = e
                            } label: {
                                Text(e)
                                    .font(.title2)
                                    .frame(width: 44, height: 44)
                                    .background(
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(emoji == e ? Color.indigo.opacity(0.15) : Color(.systemGray6))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .strokeBorder(emoji == e ? Color.indigo : Color.clear, lineWidth: 2)
                                            )
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)

                    HStack {
                        Text(String(localized: "groupEdit.label.custom"))
                            .foregroundStyle(.secondary)
                        TextField(String(localized: "groupEdit.placeholder.customEmoji"), text: $emoji)
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section(String(localized: "groupEdit.section.name")) {
                    if isCanonical {
                        Text(group?.displayName ?? name)
                            .foregroundStyle(.secondary)
                        Text(String(localized: "groupEdit.canonicalName.note",
                                    defaultValue: "This name follows your device language and can’t be changed."))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else {
                        TextField(String(localized: "groupEdit.placeholder.name"), text: $name)
                        HStack {
                            if isDuplicate {
                                Text(String(localized: "groupEdit.error.duplicate"))
                                    .font(.caption)
                                    .foregroundStyle(.red)
                            } else {
                                Spacer()
                            }
                            Text("\(name.count) / 30")
                                .font(.caption)
                                .foregroundStyle(name.count > 30 ? .red : .secondary)
                        }
                    }
                }
            }
            .navigationTitle(isEditing ? String(localized: "groupEdit.navTitle.edit") : String(localized: "groupEdit.navTitle.new"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.save")) { save() }
                        .disabled(!canSave)
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedEmoji = emoji.trimmingCharacters(in: .whitespaces).isEmpty ? "👥" : emoji
        if let group {
            if !isCanonical { group.name = trimmedName }   // canonical name is derived
            group.emoji = trimmedEmoji
            try? modelContext.save()
        } else {
            let newGroup = ContactGroup(name: trimmedName, emoji: trimmedEmoji)
            modelContext.insert(newGroup)
            try? modelContext.save()
            // TIC-88: hand the persisted group back so the Groups list can
            // auto-open Add Members for it — no extra taps to start populating.
            onCreated?(newGroup)
        }
        dismiss()
    }
}
