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
    @State private var composeContact: Contact?
    @State private var prefilledCategory: WarmCategory? = nil

    /// Deferred follow-up presentation, run from the action sheet's `onDismiss`
    /// so we never present a new sheet while the action sheet is still dismissing.
    private enum PendingAction {
        case compose(Contact)
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
        if isRegular {
            regularBody    // iPad: NavigationSplitView, two-pane
        } else {
            compactBody    // iPhone: NavigationStack + modal sheets
        }
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
                TickleEditView()
            }
            .sheet(item: $editingReminder) { reminder in
                TickleEditView(existing: reminder)
            }
            .sheet(item: $actionSheetReminder, onDismiss: runPendingAction) { reminder in
                TickleActionSheet(
                    reminder: reminder,
                    warmth: warmth,
                    onCompose: { if let contact = reminder.contact { pendingAction = .compose(contact) } },
                    onMarkDone: { TickleScheduler.markComplete(reminder: reminder, context: modelContext) },
                    onSnooze: { TickleScheduler.snooze(reminder: reminder, days: 7, context: modelContext) },
                    onEdit: { pendingAction = .edit(reminder) }
                )
                .presentationDetents([.height(TickleActionSheet.estimatedHeight(for: reminder))])
                .presentationDragIndicator(.visible)
            }
            .sheet(item: $composeContact) { contact in
                ComposeView(onClose: { composeContact = nil }, initialContact: contact)
            }
        }
    }

    /// Runs after the action sheet has fully dismissed, presenting whichever
    /// follow-up surface the user requested.
    private func runPendingAction() {
        switch pendingAction {
        case .compose(let contact): composeContact = contact
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
            .sheet(item: $composeContact) { contact in
                ComposeView(onClose: { composeContact = nil }, initialContact: contact)
            }
        } detail: {
            detailPane
        }
    }

    @ViewBuilder
    private var detailPane: some View {
        if paneAddingNew {
            TickleEditView(onClose: { paneAddingNew = false })
                .id("new-tickle")
        } else if let target = paneEditTarget {
            TickleEditView(existing: target, onClose: { paneEditTarget = nil })
                .id(target.persistentModelID)
        } else if let reminder = selectedReminder {
            TickleActionSheet(
                reminder: reminder,
                warmth: warmth,
                dismissesOnAction: false,
                onCompose: { if let contact = reminder.contact { composeContact = contact } },
                onMarkDone: {
                    TickleScheduler.markComplete(reminder: reminder, context: modelContext)
                    selectedReminder = nil
                },
                onSnooze: {
                    TickleScheduler.snooze(reminder: reminder, days: 7, context: modelContext)
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
            TickleScheduler.markComplete(reminder: reminder, context: modelContext)
        }, warmth: warmth)
        .contentShape(Rectangle())
        .tag(reminder)
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            Button {
                TickleScheduler.markComplete(reminder: reminder, context: modelContext)
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
                TickleScheduler.snooze(reminder: reminder, days: 7, context: modelContext)
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
