import SwiftUI
import SwiftData

struct TickleEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \ContactGroup.name) private var allGroups: [ContactGroup]

    let existing: TickleReminder?

    enum TargetType: Hashable { case contact, group }

    @State private var targetType: TargetType
    @State private var selectedContact: Contact?
    @State private var selectedGroup: ContactGroup?
    @State private var frequency: TickleFrequency
    @State private var customIntervalDays: Int
    @State private var startDate: Date
    @State private var note: String
    @State private var showingContactPicker = false
    @State private var showingCreateGroupSheet = false
    @State private var showToast = false
    @State private var toastMessage = ""

    private var isEditing: Bool { existing != nil }

    private var canSave: Bool {
        switch targetType {
        case .contact: return selectedContact != nil
        case .group:   return selectedGroup != nil
        }
    }

    init(contact: Contact? = nil, existing: TickleReminder? = nil) {
        self.existing = existing
        if let r = existing {
            _targetType          = State(initialValue: r.group != nil ? .group : .contact)
            _selectedContact     = State(initialValue: r.contact)
            _selectedGroup       = State(initialValue: r.group)
            _frequency           = State(initialValue: r.frequency)
            _customIntervalDays  = State(initialValue: r.customIntervalDays ?? 30)
            _startDate           = State(initialValue: r.nextDueDate)
            _note                = State(initialValue: r.note)
        } else {
            _targetType          = State(initialValue: .contact)
            _selectedContact     = State(initialValue: contact)
            _selectedGroup       = State(initialValue: nil)
            _frequency           = State(initialValue: .monthly)
            _customIntervalDays  = State(initialValue: 30)
            _startDate           = State(initialValue: Date())
            _note                = State(initialValue: "")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Who") {
                    Picker("", selection: $targetType) {
                        Text("Contact").tag(TargetType.contact)
                        Text("Group").tag(TargetType.group)
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())

                    if targetType == .contact {
                        Button {
                            showingContactPicker = true
                        } label: {
                            HStack {
                                if let c = selectedContact {
                                    Text(c.fullName).foregroundStyle(.primary)
                                } else {
                                    Text("Choose a contact").foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption).foregroundStyle(.tertiary)
                            }
                        }
                        .tint(.primary)
                    } else if allGroups.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("No groups yet")
                                .foregroundStyle(.secondary)
                            Text("Groups let you tickle everyone on a team or in a circle at once.")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Button("Create a Group") {
                                showingCreateGroupSheet = true
                            }
                            .foregroundStyle(Color(red: 0.145, green: 0.388, blue: 0.922))
                        }
                        .padding(.vertical, 4)
                    } else {
                        Picker("Group", selection: $selectedGroup) {
                            Text("Choose a group").tag(Optional<ContactGroup>.none)
                            ForEach(allGroups) { group in
                                HStack {
                                    Text(group.emoji)
                                    Text(group.name)
                                }
                                .tag(Optional(group))
                            }
                        }
                    }
                }

                Section("Schedule") {
                    Picker("Frequency", selection: $frequency) {
                        ForEach(TickleFrequency.allCases, id: \.self) { f in
                            Text(f.rawValue).tag(f)
                        }
                    }
                    if frequency == .custom {
                        Stepper("Every \(customIntervalDays) day\(customIntervalDays == 1 ? "" : "s")",
                                value: $customIntervalDays, in: 1...365)
                    }
                    DatePicker("Starting", selection: $startDate, displayedComponents: .date)
                }

                Section("Note") {
                    TextField("e.g. Ask about the new role", text: $note)
                }
            }
            .navigationTitle(isEditing ? "Edit Tickle" : "New Tickle")
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
            .sheet(isPresented: $showingContactPicker) {
                ContactPickerSheet(selected: $selectedContact)
            }
            .sheet(isPresented: $showingCreateGroupSheet) {
                GroupEditSheet(group: nil)
            }
            .overlay(alignment: .bottom) {
                if showToast {
                    Text(toastMessage)
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(red: 0.145, green: 0.388, blue: 0.922))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .shadow(radius: 4)
                        .padding(.bottom, 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: showToast)
        }
    }

    private func save() {
        TickleScheduler.requestPermissionIfNeeded()
        let trimmedNote = note.trimmingCharacters(in: .whitespaces)
        let intervalDays = frequency == .custom ? customIntervalDays : nil

        if let r = existing {
            r.contact          = targetType == .contact ? selectedContact : nil
            r.group            = targetType == .group   ? selectedGroup   : nil
            r.frequency        = frequency
            r.customIntervalDays = intervalDays
            r.nextDueDate      = startDate
            r.note             = trimmedNote
            r.status           = .active
            TickleScheduler.cancelNotification(for: r)
            TickleScheduler.scheduleNotification(for: r)
        } else {
            let reminder = TickleReminder(
                contact:            targetType == .contact ? selectedContact : nil,
                group:              targetType == .group   ? selectedGroup   : nil,
                note:               trimmedNote,
                frequency:          frequency,
                customIntervalDays: intervalDays,
                startDate:          startDate
            )
            modelContext.insert(reminder)
            TickleScheduler.scheduleNotification(for: reminder)
        }
        try? modelContext.save()
        toastMessage = isEditing ? "Tickle updated" : "Tickle saved"
        showToast = true
        Task {
            try? await Task.sleep(for: .seconds(2))
            dismiss()
        }
    }
}

// MARK: - Contact Picker Sheet

private struct ContactPickerSheet: View {
    @Binding var selected: Contact?
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Contact.lastName) private var contacts: [Contact]
    @State private var searchText = ""

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
                    selected = contact
                    dismiss()
                } label: {
                    HStack {
                        ContactRowView(contact: contact)
                        Spacer()
                        if selected?.id == contact.id {
                            Image(systemName: "checkmark").foregroundStyle(.indigo)
                        }
                    }
                }
                .tint(.primary)
            }
            .searchable(text: $searchText, prompt: "Search contacts")
            .navigationTitle("Choose Contact")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
            .overlay {
                if contacts.isEmpty {
                    ContentUnavailableView(
                        "No contacts",
                        systemImage: "person.slash",
                        description: Text("Add contacts first from the Network tab")
                    )
                } else if !searchText.isEmpty && filtered.isEmpty {
                    ContentUnavailableView.search
                }
            }
        }
    }
}
