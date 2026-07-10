import SwiftUI
import SwiftData
import UserNotifications

/// Presents tickle notifications that fire while the app is foregrounded, and
/// routes taps + Done/Snooze actions (TIC-83).
///
/// - `willPresent`: without a delegate returning presentation options, iOS
///   silently suppresses foreground notifications — so the "due today / overdue
///   → fire in 5s" branch in `TickleScheduler.scheduleNotification` showed
///   nothing (TIC-78).
/// - `didReceive`: a tap resolves a `TickleNotificationRoute` (via the store) and
///   publishes it to `ContentView`; Done/Snooze mutate the reminder in the
///   background without foregrounding the app.
///
/// The one piece of mutable state, `modelContainer`, is assigned exactly once at
/// launch (before any notification can be delivered) and only ever read on the
/// main actor, hence `@unchecked Sendable`. The store work in `didReceive` hops
/// onto the main actor with an explicit `Task { @MainActor in ... }` — never
/// `assumeIsolated` — so a callback arriving off the main thread reroutes
/// instead of crashing (see `didReceive`).
final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = NotificationPresenter()

    /// The app's single shared container. The background action path fetches a
    /// context from *this* container (`mainContext`) — never a second store —
    /// even when the system launches the app into the background just to run an
    /// action (TIC-83).
    var modelContainer: ModelContainer?

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping @Sendable () -> Void
    ) {
        // Snapshot plain values before any actor hop — never capture UN objects
        // or @Model instances.
        let (reminderID, _) = TickleNotificationRouter.ids(from: response.notification.request.content.userInfo)
        let actionID = response.actionIdentifier

        // Hop to the main actor explicitly rather than `assumeIsolated`: modern
        // SDKs document this callback as main-thread, but a background action
        // launch (the app spawned headless just to run Done/Snooze) is exactly
        // where OS-version threading quirks bite, and a wrong assumption there
        // is a dispatch-precondition crash loop. The system waits for
        // `completionHandler`, which fires after the store work lands — and
        // every path through `handle` (including the nil-container guard)
        // returns normally, so it is always called.
        Task { @MainActor in
            self.handle(actionID: actionID, reminderID: reminderID)
            completionHandler()
        }
    }

    @MainActor
    private func handle(actionID: String, reminderID: UUID?) {
        guard let context = modelContainer?.mainContext else { return }
        switch actionID {
        case TickleNotificationRouter.doneActionID:
            if let reminderID { TickleNotificationRouter.completeReminder(reminderID, context: context) }
        case TickleNotificationRouter.snoozeActionID:
            if let reminderID { TickleNotificationRouter.snoozeReminder(reminderID, context: context) }
        case UNNotificationDefaultActionIdentifier:
            // A plain tap on the banner/body — route into the app.
            let route = TickleNotificationRouter.resolveRoute(reminderID: reminderID, context: context)
            TickleNotificationCoordinator.shared.post(route)
        default:
            break // dismiss / custom-dismiss — nothing to do
        }
    }
}

@main
struct SITApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var showLaunch = true

    let modelContainer: ModelContainer = {
        let schema = Schema([
            Contact.self,
            ContactGroup.self,
            MessageTemplate.self,
            TickleReminder.self
        ])
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }()

    init() {
        NotificationPresenter.shared.modelContainer = modelContainer
        UNUserNotificationCenter.current().delegate = NotificationPresenter.shared
        // Register the Done / Snooze banner actions so scheduled tickle
        // notifications surface them (TIC-83).
        TickleNotificationRouter.registerCategories()
        MessageTemplateSeed.seedIfNeeded(container: modelContainer)
        CanonicalGroupSeed.seedIfNeeded(container: modelContainer)
        #if DEBUG
        // Automation aid: `-debugSeedDemoData YES` runs the same idempotent
        // demo seed as Settings → Debug → Load Demo Data, so simulator runs
        // can start with data without navigating the UI. Debug builds only.
        if UserDefaults.standard.bool(forKey: "debugSeedDemoData") {
            let context = ModelContext(modelContainer)
            _ = try? SeedDataService.seedDemoData(context: context)
        }
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                if showLaunch {
                    LaunchScreenView()
                        .transition(.opacity)
                } else if !hasCompletedOnboarding {
                    OnboardingView()
                        .transition(.opacity)
                } else {
                    ContentView()
                        .transition(.opacity)
                }
            }
            .animation(.easeOut(duration: 0.4), value: showLaunch)
            .animation(.easeOut(duration: 0.4), value: hasCompletedOnboarding)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    showLaunch = false
                }
            }
        }
        .modelContainer(modelContainer)
    }
}
