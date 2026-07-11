import SwiftUI
import SwiftData

struct TickleEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let existing: TickleReminder?
    /// When set (iPad detail pane, TIC-46), close via this callback instead of the
    /// sheet `dismiss()` — the pane is hosted in a `NavigationSplitView`, not presented.
    var onClose: (() -> Void)? = nil
    /// Runs right before the editor closes on a successful save, carrying the
    /// localized save-confirmation text (TIC-84). Save now applies and the
    /// editor dismisses immediately — the presenter (Tickle tab, Contact Detail,
    /// Milestones hero) is what remains on screen afterward, so it — not this
    /// sheet — shows the confirmation toast.
    var onSaved: ((String) -> Void)? = nil

    @State private var selectedContact: Contact?
    // Not `private`: TIC-91 tests assert the seeded value directly via
    // `@testable import`, which needs `internal` (module-level) access —
    // `private`/`fileprivate` stay opaque to @testable regardless of target.
    @State var frequency: TickleFrequency
    @State private var customIntervalDays: Int
    @State private var startDate: Date
    @State private var note: String
    @State private var showingContactPicker = false
    @State private var isSaving = false

    /// Schedule fields as seeded into the editor. On save we compare the live
    /// form values against these to decide whether the schedule actually changed
    /// — only then do we recompute `nextDueDate`/`status`. A note-only edit must
    /// not skip a due occurrence or un-snooze the reminder (TIC-67).
    private let initialFrequency: TickleFrequency
    private let initialCustomIntervalDays: Int
    private let initialStartDate: Date

    /// Close the editor — via the pane callback on iPad, else the sheet dismiss.
    private func close() {
        if let onClose { onClose() } else { dismiss() }
    }

    private var isEditing: Bool { existing != nil }

    /// TIC-70: a group-anchored reminder (no contact) is still saveable — an
    /// existing group link satisfies the target requirement in place of a contact.
    private var canSave: Bool {
        (selectedContact != nil || existing?.group != nil) && !isSaving
    }

    init(contact: Contact? = nil, existing: TickleReminder? = nil, prefilledCategory: WarmCategory? = nil, onClose: (() -> Void)? = nil, onSaved: ((String) -> Void)? = nil) {
        self.existing = existing
        self.onClose = onClose
        self.onSaved = onSaved
        if let r = existing {
            _selectedContact     = State(initialValue: r.contact)
            _frequency           = State(initialValue: r.frequency)
            _customIntervalDays  = State(initialValue: r.customIntervalDays ?? 30)
            _startDate           = State(initialValue: r.nextDueDate)
            _note                = State(initialValue: r.note)
            initialFrequency          = r.frequency
            initialCustomIntervalDays = r.customIntervalDays ?? 30
            initialStartDate          = r.nextDueDate
        } else {
            let seededFrequency = Self.seededFrequency(prefilledCategory: prefilledCategory)
            _selectedContact     = State(initialValue: contact)
            _frequency           = State(initialValue: seededFrequency)
            _customIntervalDays  = State(initialValue: 30)
            _startDate           = State(initialValue: Date())
            _note                = State(initialValue: "")
            initialFrequency          = seededFrequency
            initialCustomIntervalDays = 30
            initialStartDate          = Date()
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
                            } else if let group = existing?.group {
                                // TIC-70: show the anchoring group's name where the
                                // contact name would go, so the user sees the reminder
                                // is group-based rather than an empty target.
                                Text(group.displayName).foregroundStyle(.primary)
                            } else {
                                Text(String(localized: "tickleEdit.placeholder.contact")).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption).foregroundStyle(.tertiary)
                                .flipsForRightToLeftLayoutDirection(true)
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

                Section(String(localized: "tickleEdit.section.commonAnnualEvents", defaultValue: "Common annual events")) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            annualPresetButton(
                                title: String(localized: "tickleEdit.preset.birthday", defaultValue: "Birthday"),
                                note: String(localized: "tickleEdit.presetNote.birthday", defaultValue: "Birthday")
                            )
                            annualPresetButton(
                                title: String(localized: "tickleEdit.preset.anniversary", defaultValue: "Anniversary"),
                                note: String(localized: "tickleEdit.presetNote.anniversary", defaultValue: "Anniversary")
                            )
                            annualPresetButton(
                                title: String(localized: "tickleEdit.preset.specialEvent", defaultValue: "Special event"),
                                note: String(localized: "tickleEdit.presetNote.specialEvent", defaultValue: "Special event")
                            )
                        }
                    }
                }

                Section(String(localized: "tickleEdit.section.note")) {
                    TextField(String(localized: "tickleEdit.placeholder.note"), text: $note)
                }
            }
            .navigationTitle(isEditing ? String(localized: "tickleEdit.navTitle.edit") : String(localized: "tickleEdit.navTitle.new"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "common.cancel")) { close() }
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
        }
    }

    private func save() {
        // Flip BEFORE any work so the next tap sees canSave == false — the
        // editor closes immediately below, but stays disabled for whatever
        // sliver of the dismiss animation it's still on screen for, so a
        // rapid double-tap can't insert/mutate twice.
        isSaving = true
        // Notification authorization is requested (and awaited) inside
        // `TickleScheduler.scheduleNotification` itself now, so the first-ever
        // tickle's notification isn't scheduled before the permission dialog
        // has resolved (TIC-65).
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
            // TIC-70: only overwrite the target when the user explicitly picked a
            // contact. A group-anchored reminder edited without choosing one keeps
            // its group link instead of being silently unlinked.
            if let picked = selectedContact {
                r.contact = picked
                r.group   = nil
            }
            r.note             = finalNote

            // TIC-67: the schedule fields (frequency / custom interval / start
            // date) drive nextDueDate and status. Recompute those ONLY when a
            // schedule field actually changed — a note/target-only edit must not
            // skip a due occurrence or un-snooze the reminder.
            let scheduleChanged =
                frequency != initialFrequency ||
                customIntervalDays != initialCustomIntervalDays ||
                !Calendar.current.isDate(startDate, inSameDayAs: initialStartDate)
            r.frequency          = frequency
            r.customIntervalDays = intervalDays
            if scheduleChanged {
                r.startDate   = startDate
                r.nextDueDate = TickleScheduler.initialNextDueDate(
                    from: startDate,
                    frequency: frequency,
                    customDays: intervalDays
                )
                r.status = .active
            }
            TickleScheduler.cancelNotification(for: r)
            // Reschedule for active AND snoozed reminders — a snoozed reminder
            // keeps a pending notification for its snooze-end date (snooze()
            // itself schedules one), so a note-only edit must restore it.
            // Only completed reminders stay silent.
            if r.status != .completed {
                TickleScheduler.scheduleNotification(for: r)
            }
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
        // TIC-84: apply and close immediately — no artificial delay. The
        // confirmation toast is handed to whichever presenter remains on
        // screen after this sheet is gone, since it can no longer host it.
        let message = isEditing
            ? String(localized: "tickleEdit.toast.updated")
            : String(localized: "tickleEdit.toast.saved")
        onSaved?(message)
        close()
    }

    /// Resolves the frequency to seed a brand-new tickle with (TIC-91).
    ///
    /// The Milestones hero card opens the editor pre-configured for an
    /// anniversary-style reminder — `.annual`, unconditionally — so it lands
    /// meaningfully different from the plain "+" regardless of what's stored
    /// in Settings. Every other new-tickle entry point seeds from the user's
    /// Settings → Tickle Defaults preference instead of always hardcoding
    /// `.monthly`.
    ///
    /// A free function (not tied to view rendering) so it's directly unit
    /// testable without ViewInspector/ModelContainer plumbing — mirrors this
    /// file's existing pattern of pushing pure decision logic out of the body.
    static func seededFrequency(prefilledCategory: WarmCategory?, defaults: UserDefaults = .standard) -> TickleFrequency {
        guard prefilledCategory != .milestones else { return .annual }
        return resolvedDefaultFrequency(from: defaults)
    }

    /// Reads and parses the stored Settings default
    /// (`@AppStorage("defaultTickleFrequency")` in `SettingsView`, a
    /// `TickleFrequency.rawValue` string). Parses defensively: unset,
    /// unrecognized (e.g. left over from a removed case), or `.custom` values
    /// all fall back to `.monthly` — `.custom` has no stored interval to seed
    /// a new reminder with, and the Settings picker already excludes it as a
    /// selectable default for that reason.
    static func resolvedDefaultFrequency(from defaults: UserDefaults = .standard) -> TickleFrequency {
        guard let raw = defaults.string(forKey: "defaultTickleFrequency"),
              let parsed = TickleFrequency(rawValue: raw),
              parsed != .custom
        else {
            return .monthly
        }
        return parsed
    }

    private func annualPresetButton(title: String, note presetNote: String) -> some View {
        Button {
            frequency = .annual
            note = presetNote
        } label: {
            Text(title)
        }
        .buttonStyle(.bordered)
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
