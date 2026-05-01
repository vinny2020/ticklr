import SwiftUI
import SwiftData

struct TickleEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let existing: TickleReminder?

    @State private var selectedContact: Contact?
    @State private var frequency: TickleFrequency
    @State private var customIntervalDays: Int
    @State private var startDate: Date
    @State private var note: String
    @State private var showingContactPicker = false
    @State private var showToast = false
    @State private var toastMessage = ""

    private var isEditing: Bool { existing != nil }

    private var canSave: Bool { selectedContact != nil }

    init(contact: Contact? = nil, existing: TickleReminder? = nil) {
        self.existing = existing
        if let r = existing {
            _selectedContact     = State(initialValue: r.contact)
            _frequency           = State(initialValue: r.frequency)
            _customIntervalDays  = State(initialValue: r.customIntervalDays ?? 30)
            _startDate           = State(initialValue: r.nextDueDate)
            _note                = State(initialValue: r.note)
        } else {
            _selectedContact     = State(initialValue: contact)
            _frequency           = State(initialValue: .monthly)
            _customIntervalDays  = State(initialValue: 30)
            _startDate           = State(initialValue: Date())
            _note                = State(initialValue: "")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "tickleEdit.section.who")) {
                    Button {
                        showingContactPicker = true
                    } label: {
                        HStack {
                            if let c = selectedContact {
                                Text(c.fullName).foregroundStyle(.primary)
                            } else {
                                Text(String(localized: "tickleEdit.placeholder.contact")).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption).foregroundStyle(.tertiary)
                        }
                    }
                    .tint(.primary)
                }

                Section(String(localized: "tickleEdit.section.schedule")) {
                    Picker(String(localized: "tickleEdit.row.frequency"), selection: $frequency) {
                        ForEach(TickleFrequency.allCases, id: \.self) { f in
                            Text(f.localizedName).tag(f)
                        }
                    }
                    if frequency == .custom {
                        Stepper(String(localized: "tickleEdit.stepper.customInterval \(customIntervalDays)"),
                                value: $customIntervalDays, in: 1...365)
                    }
                    DatePicker(String(localized: "tickleEdit.row.starting"), selection: $startDate, displayedComponents: .date)
                }

                Section(String(localized: "tickleEdit.section.note")) {
                    TextField(String(localized: "tickleEdit.placeholder.note"), text: $note)
                }
            }
            .navigationTitle(isEditing ? String(localized: "tickleEdit.navTitle.edit") : String(localized: "tickleEdit.navTitle.new"))
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
            .sheet(isPresented: $showingContactPicker) {
                ContactPickerSheet(selected: $selectedContact)
            }
            .overlay(alignment: .bottom) {
                if showToast {
                    Text(verbatim: toastMessage)
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
        // Empty (not whitespace-only) defaults to the localized "Stay in touch"
        // so users never end up with a blank-noted reminder. A typed space is
        // intentional input — trim it instead of substituting.
        let finalNote: String
        if note.isEmpty {
            finalNote = String(localized: "tickleEdit.default.note")
        } else {
            finalNote = note.trimmingCharacters(in: .whitespaces)
        }
        let intervalDays = frequency == .custom ? customIntervalDays : nil

        if let r = existing {
            r.contact          = selectedContact
            r.group            = nil
            r.frequency        = frequency
            r.customIntervalDays = intervalDays
            r.nextDueDate = TickleScheduler.initialNextDueDate(
                from: startDate,
                frequency: frequency,
                customDays: intervalDays
            )
            r.note             = finalNote
            r.status           = .active
            TickleScheduler.cancelNotification(for: r)
            TickleScheduler.scheduleNotification(for: r)
        } else {
            let reminder = TickleReminder(
                contact:            selectedContact,
                group:              nil,
                note:               finalNote,
                frequency:          frequency,
                customIntervalDays: intervalDays,
                startDate:          startDate
            )
            modelContext.insert(reminder)
            TickleScheduler.scheduleNotification(for: reminder)
        }
        try? modelContext.save()
        toastMessage = isEditing
            ? String(localized: "tickleEdit.toast.updated")
            : String(localized: "tickleEdit.toast.saved")
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
