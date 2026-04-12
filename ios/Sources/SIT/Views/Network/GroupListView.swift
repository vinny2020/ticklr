import SwiftUI
import SwiftData

struct GroupListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ContactGroup.name) private var groups: [ContactGroup]

    @State private var showingAddGroup = false
    @State private var editingGroup: ContactGroup?

    var body: some View {
        NavigationStack {
            List {
                ForEach(groups) { group in
                    NavigationLink(destination: GroupDetailView(group: group)) {
                        HStack(spacing: 12) {
                            Text(group.emoji)
                                .font(.title3)
                                .frame(width: 40, height: 40)
                                .background(Color(.systemGray6))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(group.name)
                                Text(String(localized: "groupList.contactCount \(group.contacts.count)"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            modelContext.delete(group)
                            try? modelContext.save()
                        } label: {
                            Label(String(localized: "common.delete"), systemImage: "trash")
                        }
                        Button {
                            editingGroup = group
                        } label: {
                            Label(String(localized: "common.edit"), systemImage: "pencil")
                        }
                        .tint(.indigo)
                    }
                }
            }
            .navigationTitle(String(localized: "groupList.navTitle"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showingAddGroup = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
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
            .sheet(isPresented: $showingAddGroup) {
                GroupEditSheet(group: nil)
            }
            .sheet(item: $editingGroup) { group in
                GroupEditSheet(group: group)
            }
        }
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
        _name = State(initialValue: group?.name ?? "")
        _emoji = State(initialValue: group?.emoji ?? "👥")
    }

    private var isEditing: Bool { group != nil }
    private var isDuplicate: Bool {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return false }
        return allGroups.contains {
            $0.name.caseInsensitiveCompare(trimmed) == .orderedSame &&
            $0.id != group?.id
        }
    }
    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
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
            group.name = trimmedName
            group.emoji = trimmedEmoji
        } else {
            let newGroup = ContactGroup(name: trimmedName, emoji: trimmedEmoji)
            modelContext.insert(newGroup)
        }
        try? modelContext.save()
        dismiss()
    }
}
