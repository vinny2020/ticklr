import SwiftUI
import SwiftData

struct GroupListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ContactGroup.name) private var groups: [ContactGroup]

    @State private var showingAddGroup = false
    @State private var editingGroup: ContactGroup?
    @State private var selectedGroup: ContactGroup?

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    var body: some View {
        NavigationSplitView {
            ScrollView {
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
            .sheet(isPresented: $showingAddGroup) {
                GroupEditSheet(group: nil)
            }
            .sheet(item: $editingGroup) { group in
                GroupEditSheet(group: group)
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
        } detail: {
            if let selectedGroup {
                NavigationStack {
                    GroupDetailView(group: selectedGroup)
                }
            } else {
                ContentUnavailableView(
                    String(localized: "warm.groups.detail.empty.title",
                           defaultValue: "Pick a circle"),
                    systemImage: "person.3.sequence",
                    description: Text(String(localized: "warm.groups.detail.empty.description",
                                              defaultValue: "Choose a group to see its members."))
                )
                .background(palette.paper.ignoresSafeArea())
            }
        }
    }

    // MARK: - Pieces

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
            }
            .padding(.horizontal, WarmSpacing.lg)
            .padding(.vertical, 16)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button(String(localized: "common.edit"), systemImage: "pencil") {
                editingGroup = group
            }
            Button(String(localized: "common.delete"), systemImage: "trash", role: .destructive) {
                if selectedGroup == group { selectedGroup = nil }
                TickleScheduler.deleteGroup(group, context: modelContext)
                try? modelContext.save()
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

// MARK: - Create / Edit Sheet

struct GroupEditSheet: View {
    let group: ContactGroup?
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

    init(group: ContactGroup?) {
        self.group = group
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
        } else {
            let newGroup = ContactGroup(name: trimmedName, emoji: trimmedEmoji)
            modelContext.insert(newGroup)
        }
        try? modelContext.save()
        dismiss()
    }
}
