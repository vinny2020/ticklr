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
    #if DEBUG
    @State private var showClearConfirm = false
    #endif

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
                Section(String(localized: "settings.section.data")) {
                    LabeledContent(String(localized: "settings.row.contacts"), value: contacts.count.formatted())
                    NavigationLink(String(localized: "settings.row.importContacts")) {
                        ImportView()
                    }
                    NavigationLink(String(localized: "settings.row.messageTemplates")) {
                        TemplateListView()
                    }
                }

                // MARK: — Notifications
                Section {
                    if systemAuthStatus == .denied {
                        LabeledContent(String(localized: "settings.row.tickleReminders")) {
                            Button(String(localized: "settings.button.enableInSettings")) {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                            .font(.subheadline)
                        }
                    } else {
                        Toggle(String(localized: "settings.row.tickleReminders"), isOn: $notificationsEnabled)
                            .onChange(of: notificationsEnabled) { _, enabled in
                                handleNotificationToggle(enabled: enabled)
                            }
                    }
                } header: {
                    Text(String(localized: "settings.section.notifications"))
                } footer: {
                    if systemAuthStatus == .denied {
                        Text(String(localized: "settings.footer.notificationsDenied"))
                    } else if !notificationsEnabled {
                        Text(String(localized: "settings.footer.notificationsOff"))
                    }
                }

                // MARK: — Tickle Defaults
                Section(String(localized: "settings.section.tickleDefaults")) {
                    Picker(String(localized: "settings.row.defaultFrequency"), selection: defaultFrequency) {
                        ForEach(TickleFrequency.allCases.filter { $0 != .custom }, id: \.self) { freq in
                            Text(freq.localizedName).tag(freq)
                        }
                    }
                }

                // MARK: — About
                Section {
                    WordmarkLockup()
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets())
                    LabeledContent(String(localized: "settings.row.version"), value: appVersion)
                    LabeledContent(String(localized: "settings.row.builtBy"), value: "Xaymaca")
                } header: {
                    Text(String(localized: "settings.section.about"))
                }

                // MARK: — Reset
                Section {
                    Button(String(localized: "settings.button.resetOnboarding"), role: .destructive) {
                        hasCompletedOnboarding = false
                    }
                }

                // MARK: — Debug (compiled out in Release)
                #if DEBUG
                Section {
                    Button("Load Test Contacts") {
                        Task { @MainActor in
                            do {
                                let result = try SeedDataService.seedTestContacts(context: modelContext)
                                switch (result.imported, result.skipped) {
                                case (_, 0):
                                    seedMessage = "Loaded \(result.imported) test contacts ✓"
                                case (0, _):
                                    seedMessage = "All \(result.skipped) contacts already exist"
                                default:
                                    seedMessage = "Loaded \(result.imported) new, skipped \(result.skipped) duplicates"
                                }
                            } catch {
                                seedMessage = "Seed failed: \(error.localizedDescription)"
                            }
                        }
                    }
                    .foregroundColor(.yellow)

                    Button("Clear All Data", role: .destructive) {
                        showClearConfirm = true
                    }

                    if let msg = seedMessage {
                        Text(msg)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text("Debug")
                } footer: {
                    Text("Debug only — not visible in release builds.")
                }
                .confirmationDialog(
                    "Clear all data?",
                    isPresented: $showClearConfirm,
                    titleVisibility: .visible
                ) {
                    Button("Delete All Data", role: .destructive) {
                        Task { @MainActor in
                            let contacts = (try? modelContext.fetch(FetchDescriptor<Contact>())) ?? []
                            for contact in contacts { modelContext.delete(contact) }
                            let groups = (try? modelContext.fetch(FetchDescriptor<ContactGroup>())) ?? []
                            for group in groups { modelContext.delete(group) }
                            let tickles = (try? modelContext.fetch(FetchDescriptor<TickleReminder>())) ?? []
                            for tickle in tickles { modelContext.delete(tickle) }
                            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
                            try? modelContext.save()
                            seedMessage = "All data cleared ✓"
                        }
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This will permanently delete all contacts, groups, and tickles. This cannot be undone.")
                }
                #endif
            }
            .navigationTitle(String(localized: "settings.navTitle"))
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
