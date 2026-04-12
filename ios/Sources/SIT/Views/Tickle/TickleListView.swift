import SwiftUI
import SwiftData

struct TickleListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allReminders: [TickleReminder]

    @State private var showingAdd = false
    @State private var editingReminder: TickleReminder?

    private let amber = Color(red: 0.96, green: 0.78, blue: 0.25)

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
                if !dueAndOverdue.isEmpty {
                    Section {
                        ForEach(dueAndOverdue) { reminder in
                            row(for: reminder)
                        }
                    } header: {
                        Label(String(localized: "tickleList.section.due"), systemImage: "clock.badge.exclamationmark")
                            .foregroundStyle(amber)
                            .font(.subheadline.weight(.semibold))
                            .textCase(nil)
                    }
                }

                if !upcoming.isEmpty {
                    Section(String(localized: "tickleList.section.upcoming")) {
                        ForEach(upcoming) { reminder in
                            row(for: reminder)
                        }
                    }
                }

                if !snoozed.isEmpty {
                    Section(String(localized: "tickleList.section.snoozed")) {
                        ForEach(snoozed) { reminder in
                            row(for: reminder)
                                .opacity(0.55)
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(String(localized: "tickleList.navTitle"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingAdd = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .overlay {
                if allReminders.isEmpty {
                    ContentUnavailableView(
                        String(localized: "tickleList.empty.title"),
                        systemImage: "bell.badge",
                        description: Text(String(localized: "tickleList.empty.description"))
                    )
                }
            }
            .sheet(isPresented: $showingAdd) {
                TickleEditView()
            }
            .sheet(item: $editingReminder) { reminder in
                TickleEditView(existing: reminder)
            }
        }
    }

    @ViewBuilder
    private func row(for reminder: TickleReminder) -> some View {
        TickleRowView(reminder: reminder) {
            TickleScheduler.markComplete(reminder: reminder, context: modelContext)
        }
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            Button {
                TickleScheduler.markComplete(reminder: reminder, context: modelContext)
            } label: {
                Label(String(localized: "common.done"), systemImage: "checkmark")
            }
            .tint(amber)
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
            .tint(.indigo)
            Button {
                TickleScheduler.snooze(reminder: reminder, days: 7, context: modelContext)
            } label: {
                Label(String(localized: "tickleList.action.snooze"), systemImage: "zzz")
            }
            .tint(.orange)
        }
    }
}
