import SwiftUI
import SwiftData
import UserNotifications

/// Presents tickle notifications that fire while the app is foregrounded.
/// Without a delegate returning presentation options, iOS silently suppresses
/// foreground notifications — so the "due today / overdue → fire in 5s" branch
/// in `TickleScheduler.scheduleNotification` showed nothing (TIC-78). Holds no
/// mutable state, so it is safe to hand to `UNUserNotificationCenter` as a
/// shared delegate invoked off the main actor.
final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = NotificationPresenter()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
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
        UNUserNotificationCenter.current().delegate = NotificationPresenter.shared
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
