import SwiftUI
import SwiftData

enum AppTab {
    case network, tickle, groups, compose, settings
}

struct ContentView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var selectedTab: AppTab = .tickle

    @Environment(\.modelContext) private var modelContext
    /// Notification-tap routing bridge (TIC-83). A tap resolves a route in the
    /// delegate and posts it here; we consume it to present Compose or fall back
    /// to the Tickle tab — independent of whichever tab the app restored to.
    @ObservedObject private var routeCoordinator = TickleNotificationCoordinator.shared
    /// Non-nil while Compose is presented from a notification tap. Presented as a
    /// sheet (not the Compose tab) so it works over any restored tab; carries the
    /// due reminder so a send auto-completes it via TIC-82's path.
    @State private var composeRoute: ComposeRoute?
    /// Drives the "Tickle marked done — Undo" toast after a notification-tap send
    /// auto-completes the due tickle (TIC-82).
    @State private var completionSnapshot: TickleScheduler.CompletionSnapshot?

    /// A resolved notification-tap compose target: the live contact to address,
    /// paired with the due reminder a send should complete.
    private struct ComposeRoute: Identifiable {
        let id = UUID()
        let contact: Contact
        let reminder: TickleReminder
    }

    var body: some View {
        if hasCompletedOnboarding {
            TabView(selection: $selectedTab) {
                NetworkListView()
                    .tabItem { Label(String(localized: "tab.network"), systemImage: "person.2.fill") }
                    .tag(AppTab.network)
                GroupListView()
                    .tabItem { Label(String(localized: "tab.groups"), systemImage: "person.3.fill") }
                    .tag(AppTab.groups)
                TickleListView()
                    .tabItem { Label(String(localized: "tab.tickle"), systemImage: "bell.badge.fill") }
                    .tag(AppTab.tickle)
                ComposeView(onClose: { selectedTab = .tickle })
                    .tabItem { Label(String(localized: "tab.compose"), systemImage: "square.and.pencil") }
                    .tag(AppTab.compose)
                SettingsView()
                    .tabItem { Label(String(localized: "tab.settings"), systemImage: "gearshape.fill") }
                    .tag(AppTab.settings)
            }
            .tint(.indigo)
            .sheet(item: $composeRoute) { route in
                ComposeView(
                    onClose: { composeRoute = nil },
                    initialContact: route.contact,
                    dueReminder: route.reminder,
                    onTickleCompleted: { completionSnapshot = $0 }
                )
            }
            .tickleCompletionToast(snapshot: $completionSnapshot, warmth: .subtle) {
                if let snapshot = completionSnapshot {
                    TickleScheduler.undoCompletion(snapshot, context: modelContext)
                }
            }
            // Warm tap (UI already live) fires onChange; cold start (route set
            // before this view appeared) is caught by onAppear.
            .onAppear { consumePendingRoute() }
            .onChange(of: routeCoordinator.pendingRoute) { _, _ in consumePendingRoute() }
        } else {
            OnboardingView()
        }
    }

    /// Applies a pending notification-tap route, then clears it. Compose is
    /// re-materialized from the live store here (the delegate only forwarded IDs);
    /// if the objects vanished in the meantime we fall back to the Tickle tab.
    private func consumePendingRoute() {
        guard let route = routeCoordinator.pendingRoute else { return }
        routeCoordinator.pendingRoute = nil

        switch route {
        case .tickleTab:
            selectedTab = .tickle
        case let .compose(reminderID, contactID):
            selectedTab = .tickle
            if let contact = fetchContact(contactID),
               let reminder = TickleNotificationRouter.fetchReminder(reminderID, context: modelContext) {
                composeRoute = ComposeRoute(contact: contact, reminder: reminder)
            }
        }
    }

    private func fetchContact(_ id: UUID) -> Contact? {
        let descriptor = FetchDescriptor<Contact>(predicate: #Predicate { $0.id == id })
        return try? modelContext.fetch(descriptor).first
    }
}

#Preview {
    ContentView()
}

#Preview {
    ContentView()
}
