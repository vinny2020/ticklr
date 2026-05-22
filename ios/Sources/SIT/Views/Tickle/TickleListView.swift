import SwiftUI
import SwiftData

struct TickleListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allReminders: [TickleReminder]

    @State private var showingAdd = false
    @State private var editingReminder: TickleReminder?
    @State private var actionSheetReminder: TickleReminder?
    @State private var pendingAction: PendingAction?
    @State private var composeContact: Contact?
    @State private var prefilledCategory: WarmCategory? = nil

    /// Deferred follow-up presentation, run from the action sheet's `onDismiss`
    /// so we never present a new sheet while the action sheet is still dismissing.
    private enum PendingAction {
        case compose(Contact)
        case edit(TickleReminder)
    }

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

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
        NavigationStack {
            List {
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
                        onTap: {
                            prefilledCategory = .milestones
                            showingAdd = true
                        }
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
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        prefilledCategory = nil
                        showingAdd = true
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(palette.ink)
                    }
                }
            }
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
                ComposeView(onCancel: { composeContact = nil }, initialContact: contact)
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
        TickleRowView(reminder: reminder, onComplete: {
            TickleScheduler.markComplete(reminder: reminder, context: modelContext)
        }, warmth: warmth)
        .contentShape(Rectangle())
        .onTapGesture { actionSheetReminder = reminder }
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
                modelContext.delete(reminder)
                try? modelContext.save()
            } label: {
                Label(String(localized: "common.delete"), systemImage: "trash")
            }
            Button {
                editingReminder = reminder
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
