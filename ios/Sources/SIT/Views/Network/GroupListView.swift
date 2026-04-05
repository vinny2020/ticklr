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
                                Text("\(group.contacts.count) contact\(group.contacts.count == 1 ? "" : "s")")
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
                            Label("Delete", systemImage: "trash")
                        }
                        Button {
                            editingGroup = group
                        } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        .tint(.indigo)
                    }
                }
            }
            .navigationTitle("Groups")
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
                        "No groups yet",
                        systemImage: "person.3.sequence",
                        description: Text("Tap + to create your first group")
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
                Section("Emoji") {
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
                        Text("Custom")
                            .foregroundStyle(.secondary)
                        TextField("or type any emoji", text: $emoji)
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section("Name") {
                    TextField("Group name", text: $name)
                    HStack {
                        if isDuplicate {
                            Text("A group with this name already exists")
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
            .navigationTitle(isEditing ? "Edit Group" : "New Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { save() }
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
