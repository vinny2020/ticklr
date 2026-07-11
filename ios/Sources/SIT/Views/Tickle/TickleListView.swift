import SwiftUI
import SwiftData

struct TickleListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allReminders: [TickleReminder]

    // MARK: Compact (iPhone) — modal sheet flow
    @State private var showingAdd = false
    @State private var editingReminder: TickleReminder?
    @State private var actionSheetReminder: TickleReminder?
    @State private var pendingAction: PendingAction?

    // MARK: Regular (iPad) — two-pane flow (TIC-46)
    /// List selection drives the detail pane's connect actions.
    @State private var selectedReminder: TickleReminder?
    /// Non-nil while editing an existing reminder in the detail pane.
    @State private var paneEditTarget: TickleReminder?
    /// True while a new tickle is being created in the detail pane.
    @State private var paneAddingNew = false

    // MARK: Shared
    @State private var composeTarget: ComposeTarget?
    @State private var prefilledCategory: WarmCategory? = nil
    /// Non-nil while the "Tickle marked done — Undo" toast is showing — after a
    /// send auto-completed a due tickle (TIC-82) or any in-app mark-done (TIC-87:
    /// row checkmark, leading swipe, action sheet, iPad pane).
    @State private var completionSnapshot: TickleScheduler.CompletionSnapshot?
    /// Non-nil while the "Tickle snoozed — Undo" toast is showing after an in-app
    /// snooze (TIC-87). Kept separate from `completionSnapshot` so the two toasts
    /// carry different copy; only one is ever non-nil at a time (latest wins).
    @State private var snoozeSnapshot: TickleScheduler.CompletionSnapshot?
    /// Non-nil while a plain save-confirmation toast is showing after a
    /// TickleEditView sheet (add, edit, or the Milestones hero) or the iPad
    /// detail-pane editor dismissed (TIC-84).
    @State private var saveToastMessage: String?
    /// Drives the "Create a tickle for [name]?" offer toast after a plain
    /// (reminder-less) send (TIC-86). In practice a compose launched from here
    /// always carries a due reminder, so this only fires in the edge case where
    /// none was attached — wired for parity with `onTickleCompleted`.
    @State private var tickleSuggestion: TickleSuggestion?
    /// The contact whose new-tickle editor is presented when the offer is tapped.
    @State private var suggestionEditContact: Contact?

    /// A contact to compose to, paired with the due tickle (if any) that a send
    /// should auto-complete. Identifiable so it can drive a `.sheet(item:)`.
    private struct ComposeTarget: Identifiable {
        let id = UUID()
        let contact: Contact
        let reminder: TickleReminder?
    }

    /// Deferred follow-up presentation, run from the action sheet's `onDismiss`
    /// so we never present a new sheet while the action sheet is still dismissing.
    private enum PendingAction {
        case compose(TickleReminder)
        case edit(TickleReminder)
    }

    @Environment(\.horizontalSizeClass) private var hSize

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    private var isRegular: Bool { hSize == .regular }

    private var dueAndOverdue: [TickleReminder] {
        allReminders
            .filter { $0.status == .active && $0.nextDueDate <= Date() }
            .sorted { $0.nextDueDate < $1.nextDueDate }
    }

    private var upcoming: [TickleReminder] {
        allReminders
            .filter { $0.status == .active && $0.nextDueDate > Date() }
            .sorted { $0.nextDueDate < $1.nextDueDate }
    }

    private var snoozed: [TickleReminder] {
        allReminders
            .filter { $0.status == .snoozed }
            .sorted { $0.nextDueDate < $1.nextDueDate }
    }

    var body: some View {
        Group {
            if isRegular {
                regularBody    // iPad: NavigationSplitView, two-pane
            } else {
                compactBody    // iPhone: NavigationStack + modal sheets
            }
        }
        .tickleCompletionToast(snapshot: $completionSnapshot, warmth: warmth) {
            if let snapshot = completionSnapshot {
                TickleScheduler.undoCompletion(snapshot, context: modelContext)
            }
        }
        .tickleSnoozeToast(snapshot: $snoozeSnapshot, warmth: warmth) {
            if let snapshot = snoozeSnapshot {
                TickleScheduler.undoSnooze(snapshot, context: modelContext)
            }
        }
        .saveConfirmationToast(message: $saveToastMessage, warmth: warmth)
        .suggestTickleToast(suggestion: $tickleSuggestion, warmth: warmth) { suggestion in
            suggestionEditContact = fetchContact(suggestion.contactID)
        }
        .sheet(item: $suggestionEditContact) { contact in
            TickleEditView(contact: contact, onSaved: { saveToastMessage = $0 })
        }
    }

    private func fetchContact(_ id: UUID) -> Contact? {
        let descriptor = FetchDescriptor<Contact>(predicate: #Predicate { $0.id == id })
        return try? modelContext.fetch(descriptor).first
    }

    // MARK: - Mark-done / snooze with undo (TIC-87)

    /// Completes `reminder`, snapshotting its prior state and surfacing the
    /// mark-done undo toast. Clears any pending snooze toast so only one is up.
    private func markDone(_ reminder: TickleReminder) {
        snoozeSnapshot = nil
        completionSnapshot = TickleScheduler.completeWithSnapshot(reminder: reminder, context: modelContext)
    }

    /// Snoozes `reminder` by `days`, snapshotting its prior state and surfacing the
    /// snooze undo toast. Clears any pending mark-done toast so only one is up.
    private func snoozeReminder(_ reminder: TickleReminder, days: Int) {
        completionSnapshot = nil
        snoozeSnapshot = TickleScheduler.snoozeWithSnapshot(reminder: reminder, days: days, context: modelContext)
    }

    // MARK: - Compact (iPhone)

    private var compactBody: some View {
        NavigationStack {
            List {
                listSections
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { addToolbarItem }
            .sheet(isPresented: $showingAdd) {
                TickleEditView(prefilledCategory: prefilledCategory, onSaved: { saveToastMessage = $0 })
            }
            .sheet(item: $editingReminder) { reminder in
                TickleEditView(existing: reminder, onSaved: { saveToastMessage = $0 })
            }
            .sheet(item: $actionSheetReminder, onDismiss: runPendingAction) { reminder in
                TickleActionSheet(
                    reminder: reminder,
                    warmth: warmth,
                    onCompose: { pendingAction = .compose(reminder) },
                    onMarkDone: { markDone(reminder) },
                    onSnooze: { days in snoozeReminder(reminder, days: days) },
                    onEdit: { pendingAction = .edit(reminder) }
                )
                .presentationDetents([.height(TickleActionSheet.estimatedHeight(for: reminder))])
                .presentationDragIndicator(.visible)
            }
            .sheet(item: $composeTarget) { target in
                ComposeView(
                    onClose: { composeTarget = nil },
                    initialContact: target.contact,
                    dueReminder: target.reminder,
                    onTickleCompleted: { completionSnapshot = $0 },
                    onSuggestTickle: { tickleSuggestion = $0 }
                )
            }
        }
    }

    /// Runs after the action sheet has fully dismissed, presenting whichever
    /// follow-up surface the user requested.
    private func runPendingAction() {
        switch pendingAction {
        case .compose(let reminder):
            if let contact = reminder.contact {
                composeTarget = ComposeTarget(contact: contact, reminder: reminder)
            }
        case .edit(let reminder):   editingReminder = reminder
        case .none:                 break
        }
        pendingAction = nil
    }

    // MARK: - Regular (iPad) — two-pane

    private var regularBody: some View {
        NavigationSplitView {
            List(selection: $selectedReminder) {
                listSections
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { addToolbarItem }
            .sheet(item: $composeTarget) { target in
                ComposeView(
                    onClose: { composeTarget = nil },
                    initialContact: target.contact,
                    dueReminder: target.reminder,
                    onTickleCompleted: { completionSnapshot = $0 },
                    onSuggestTickle: { tickleSuggestion = $0 }
                )
            }
        } detail: {
            detailPane
        }
    }

    @ViewBuilder
    private var detailPane: some View {
        if paneAddingNew {
            TickleEditView(
                prefilledCategory: prefilledCategory,
                onClose: { paneAddingNew = false },
                onSaved: { saveToastMessage = $0 }
            )
                .id("new-tickle")
        } else if let target = paneEditTarget {
            TickleEditView(
                existing: target,
                onClose: { paneEditTarget = nil },
                onSaved: { saveToastMessage = $0 }
            )
                .id(target.persistentModelID)
        } else if let reminder = selectedReminder {
            TickleActionSheet(
                reminder: reminder,
                warmth: warmth,
                dismissesOnAction: false,
                onCompose: {
                    if let contact = reminder.contact {
                        composeTarget = ComposeTarget(contact: contact, reminder: reminder)
                    }
                },
                onMarkDone: {
                    markDone(reminder)
                    selectedReminder = nil
                },
                onSnooze: { days in
                    snoozeReminder(reminder, days: days)
                    selectedReminder = nil
                },
                onEdit: { paneEditTarget = reminder }
            )
            .id(reminder.persistentModelID)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(palette.paper.ignoresSafeArea())
        } else {
            ContentUnavailableView(
                String(localized: "warm.tickle.detail.empty.title",
                       defaultValue: "Pick a tickle"),
                systemImage: "bell",
                description: Text(String(localized: "warm.tickle.detail.empty.description",
                                         defaultValue: "Choose a reminder to see ways to reach out."))
            )
            .background(palette.paper.ignoresSafeArea())
        }
    }

    // MARK: - Shared list content

    private var addToolbarItem: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                startNewTickle(category: nil)
            } label: {
                Image(systemName: "plus")
                    .foregroundStyle(palette.ink)
            }
        }
    }

    /// Open a new-tickle editor — in the detail pane on iPad, as a sheet on iPhone.
    private func startNewTickle(category: WarmCategory?) {
        prefilledCategory = category
        if isRegular {
            paneEditTarget = nil
            selectedReminder = nil
            paneAddingNew = true
        } else {
            showingAdd = true
        }
    }

    @ViewBuilder
    private var listSections: some View {
        // MARK: Inline title + subtitle
        Section {
            VStack(alignment: .leading, spacing: 4) {
                Text(String(localized: "tickleList.navTitle"))
                    .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                    .tracking(WarmHeadingFont.tracking(warmth: warmth))
                    .foregroundStyle(palette.ink)
                if !dueAndOverdue.isEmpty {
                    Text(String(localized: "warm.tickle.subtitle",
                                defaultValue: "\(dueAndOverdue.count) to reach out to today."))
                        .font(.system(size: 14))
                        .foregroundStyle(palette.ink2)
                } else {
                    Text(String(localized: "warm.tickle.subtitle.empty",
                                defaultValue: "All caught up — no tickles due today."))
                        .font(.system(size: 14))
                        .foregroundStyle(palette.ink2)
                }
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
        }

        // MARK: Pinned Milestones hero
        Section {
            WarmCard(
                category: .milestones,
                variant: .hero,
                warmth: warmth,
                showPrompt: true,
                onTap: { startNewTickle(category: .milestones) }
            )
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 12, trailing: 16))
        }

        // MARK: Due / overdue
        if !dueAndOverdue.isEmpty {
            sectionBlock(title: String(localized: "tickleList.section.due"),
                         rows: dueAndOverdue)
        }

        // MARK: Upcoming
        if !upcoming.isEmpty {
            sectionBlock(title: String(localized: "tickleList.section.upcoming"),
                         rows: upcoming)
        }

        // MARK: Snoozed
        if !snoozed.isEmpty {
            sectionBlock(title: String(localized: "tickleList.section.snoozed"),
                         rows: snoozed, dimmed: true)
        }
    }

    @ViewBuilder
    private func sectionBlock(title: String,
                              rows: [TickleReminder],
                              dimmed: Bool = false) -> some View {
        Section {
            ForEach(Array(rows.enumerated()), id: \.element.persistentModelID) { idx, reminder in
                row(for: reminder)
                    .opacity(dimmed ? 0.55 : 1)
                    .listRowBackground(palette.cardBg)
                    .listRowSeparatorTint(palette.cardBorder)
                    .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            }
        } header: {
            WarmEyebrow(text: title, warmth: warmth)
                .textCase(nil)
                .padding(.bottom, 4)
                .padding(.top, 8)
                .padding(.horizontal, 4)
        }
    }

    @ViewBuilder
    private func row(for reminder: TickleReminder) -> some View {
        let content = TickleRowView(reminder: reminder, onComplete: {
            markDone(reminder)
        }, warmth: warmth)
        .contentShape(Rectangle())
        .tag(reminder)
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            Button {
                markDone(reminder)
            } label: {
                Label(String(localized: "common.done"), systemImage: "checkmark")
            }
            .tint(WarmCategory.resolve(for: reminder).palette.accent)
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                TickleScheduler.cancelNotification(for: reminder)
                if selectedReminder == reminder { selectedReminder = nil }
                if paneEditTarget == reminder { paneEditTarget = nil }
                modelContext.delete(reminder)
                try? modelContext.save()
            } label: {
                Label(String(localized: "common.delete"), systemImage: "trash")
            }
            Button {
                if isRegular {
                    selectedReminder = reminder
                    paneEditTarget = reminder
                } else {
                    editingReminder = reminder
                }
            } label: {
                Label(String(localized: "common.edit"), systemImage: "pencil")
            }
            .tint(palette.ink2)
            Button {
                snoozeReminder(reminder, days: 7)
            } label: {
                Label(String(localized: "tickleList.action.snooze"), systemImage: "zzz")
            }
            .tint(.orange)
        }

        // iPad uses List selection to drive the detail pane; iPhone taps open the
        // modal action sheet. (A tap gesture here would swallow List selection.)
        if isRegular {
            content
        } else {
            content.onTapGesture { actionSheetReminder = reminder }
        }
    }
}

// MARK: - Helpers

private extension WarmCategory {
    static func resolve(for reminder: TickleReminder) -> WarmCategory {
        if let contact = reminder.contact { return resolve(for: contact) }
        if let group = reminder.group, let cat = from(groupId: group.id) { return cat }
        return .community
    }
}
