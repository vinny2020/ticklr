import SwiftUI
import SwiftData
import UserNotifications

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = true
    @AppStorage("tickleNotificationsEnabled") private var notificationsEnabled = true
    @AppStorage("defaultTickleFrequency") private var defaultFrequencyRaw = TickleFrequency.monthly.rawValue

    @Query private var contacts: [Contact]

    @State private var systemAuthStatus: UNAuthorizationStatus = .notDetermined
    @State private var seedMessage: String? = nil

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    private var defaultFrequency: Binding<TickleFrequency> {
        Binding(
            get: { TickleFrequency(rawValue: defaultFrequencyRaw) ?? .monthly },
            set: { defaultFrequencyRaw = $0.rawValue }
        )
    }

    var body: some View {
        NavigationStack {
            List {
                // MARK: — Data
                Section("Data") {
                    LabeledContent("Contacts", value: contacts.count.formatted())
                    NavigationLink("Import Contacts") {
                        ImportView()
                    }
                    NavigationLink("Message Templates") {
                        TemplateListView()
                    }
                }

                // MARK: — Notifications
                Section {
                    if systemAuthStatus == .denied {
                        LabeledContent("Tickle Reminders") {
                            Button("Enable in Settings") {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                            .font(.subheadline)
                        }
                    } else {
                        Toggle("Tickle Reminders", isOn: $notificationsEnabled)
                            .onChange(of: notificationsEnabled) { _, enabled in
                                handleNotificationToggle(enabled: enabled)
                            }
                    }
                } header: {
                    Text("Notifications")
                } footer: {
                    if systemAuthStatus == .denied {
                        Text("Notification access was denied. Enable it in iOS Settings to receive tickle reminders.")
                    } else if !notificationsEnabled {
                        Text("Tickle notifications are off. You can still view due reminders in the app.")
                    }
                }

                // MARK: — Tickle Defaults
                Section("Tickle Defaults") {
                    Picker("Default Frequency", selection: defaultFrequency) {
                        ForEach(TickleFrequency.allCases.filter { $0 != .custom }, id: \.self) { freq in
                            Text(freq.rawValue).tag(freq)
                        }
                    }
                }

                // MARK: — About
                Section("About") {
                    LabeledContent("App", value: "Ticklr")
                    LabeledContent("Version", value: appVersion)
                    LabeledContent("Built by", value: "Xaymaca")
                }

                // MARK: — Reset
                Section {
                    Button("Reset Onboarding", role: .destructive) {
                        hasCompletedOnboarding = false
                    }
                }

                // MARK: — Debug (compiled out in Release)
                #if DEBUG
                Section {
                    Button("Load Test Contacts") {
                        do {
                            try SeedDataService.seedTestContacts(context: modelContext)
                            seedMessage = "Test contacts loaded ✓"
                        } catch {
                            seedMessage = "Seed failed: \(error.localizedDescription)"
                        }
                    }
                    .foregroundColor(.yellow)
                    if let msg = seedMessage {
                        Text(msg)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text("Debug")
                } footer: {
                    Text("Seeds 20 fake contacts from bundled CSV. Visible in debug builds only.")
                }
                #endif
            }
            .navigationTitle("Settings")
            .task { await fetchNotificationStatus() }
        }
    }

    // MARK: — Helpers

    private func fetchNotificationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        systemAuthStatus = settings.authorizationStatus
    }

    private func handleNotificationToggle(enabled: Bool) {
        if enabled {
            Task {
                let center = UNUserNotificationCenter.current()
                let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
                await fetchNotificationStatus()
                if granted {
                    let all = (try? modelContext.fetch(FetchDescriptor<TickleReminder>())) ?? []
                    for reminder in all where reminder.status == .active {
                        TickleScheduler.scheduleNotification(for: reminder)
                    }
                } else {
                    notificationsEnabled = false
                }
            }
        } else {
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        }
    }
}
