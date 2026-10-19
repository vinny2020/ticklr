import SwiftUI

/// Tutorial examples are plain display values, never SwiftData models. Opening a
/// guide cannot insert contacts, schedule notifications, or address a message.
enum TutorialTopic: String, CaseIterable, Identifiable {
    case phone, linkedin, tickle, groups
    var id: String { rawValue }
    var dismissalKey: String { "tutorial.dismissed.\(rawValue)" }

    var title: String {
        switch self {
        case .phone: String(localized: "tutorial.phone.title", defaultValue: "Import phone contacts", table: "Tutorials")
        case .linkedin: String(localized: "tutorial.linkedin.title", defaultValue: "Import LinkedIn connections", table: "Tutorials")
        case .tickle: String(localized: "tutorial.tickle.title", defaultValue: "Create your first tickle", table: "Tutorials")
        case .groups: String(localized: "tutorial.groups.title", defaultValue: "Organize people into groups", table: "Tutorials")
        }
    }
    var symbol: String {
        switch self {
        case .phone: "person.crop.rectangle.stack"
        case .linkedin: "doc.text"
        case .tickle: "bell.fill"
        case .groups: "person.3.fill"
        }
    }
    var actionTitle: String {
        switch self {
        case .phone: String(localized: "tutorial.phone.action", defaultValue: "Import contacts", table: "Tutorials")
        case .linkedin: String(localized: "tutorial.linkedin.action", defaultValue: "Choose CSV file", table: "Tutorials")
        case .tickle: String(localized: "warm.contact.createTickle", defaultValue: "Create a tickle")
        case .groups: String(localized: "tutorial.groups.action", defaultValue: "Explore groups", table: "Tutorials")
        }
    }
    var pages: [(title: String, body: String)] {
        switch self {
        case .phone: [
            (String(localized: "tutorial.phone.1.title", defaultValue: "Your people, already here", table: "Tutorials"),
             String(localized: "tutorial.phone.1.body", defaultValue: "Start with the contacts on your iPhone. Your Ticklr network stays on this device.", table: "Tutorials")),
            (String(localized: "tutorial.phone.2.title", defaultValue: "Choose who to bring in", table: "Tutorials"),
             String(localized: "tutorial.phone.2.body", defaultValue: "Choose individual people with the contact picker, or import all contacts Ticklr can access.", table: "Tutorials")),
            (String(localized: "tutorial.phone.3.title", defaultValue: "Make your first connection", table: "Tutorials"),
             String(localized: "tutorial.phone.3.body", defaultValue: "Find your imported people in Network. Open a profile to create a tickle or add them to a group.", table: "Tutorials"))
        ]
        case .linkedin: [
            (String(localized: "tutorial.linkedin.1.title", defaultValue: "Keep connections warm", table: "Tutorials"),
             String(localized: "tutorial.linkedin.1.body", defaultValue: "Bring professional connections into Ticklr using your LinkedIn connections CSV.", table: "Tutorials")),
            (String(localized: "tutorial.linkedin.2.title", defaultValue: "Start with your export", table: "Tutorials"),
             String(localized: "tutorial.linkedin.2.body", defaultValue: "Request a copy of your connections data from LinkedIn, then save the downloaded CSV in Files. The import screen has the full export instructions.", table: "Tutorials")),
            (String(localized: "tutorial.linkedin.3.title", defaultValue: "One file, a familiar network", table: "Tutorials"),
             String(localized: "tutorial.linkedin.3.body", defaultValue: "Choose your connections CSV. After import, your connections appear in Network, ready for a tickle.", table: "Tutorials"))
        ]
        case .tickle: [
            (String(localized: "tutorial.tickle.1.title", defaultValue: "A little reminder to reconnect", table: "Tutorials"),
             String(localized: "tutorial.tickle.1.body", defaultValue: "Open someone’s profile and tap Create a tickle. Start with a person you’d like to hear from more often.", table: "Tutorials")),
            (String(localized: "tutorial.tickle.2.title", defaultValue: "Pick a rhythm or a special date", table: "Tutorials"),
             String(localized: "tutorial.tickle.2.body", defaultValue: "Choose a frequency and starting date. For a birthday, tap Birthday and set the date; the tickle repeats annually.", table: "Tutorials")),
            (String(localized: "tutorial.tickle.3.title", defaultValue: "Remember the little things", table: "Tutorials"),
             String(localized: "tutorial.tickle.3.body", defaultValue: "Add a note, then save. Your tickle appears on the profile and in the Tickle tab. You can allow notifications when you save your first tickle.", table: "Tutorials"))
        ]
        case .groups: [
            (String(localized: "tutorial.groups.1.title", defaultValue: "A place for every circle", table: "Tutorials"),
             String(localized: "tutorial.groups.1.body", defaultValue: "Keep family, friends, and work connections together in groups.", table: "Tutorials")),
            (String(localized: "tutorial.groups.2.title", defaultValue: "Put your people together", table: "Tutorials"),
             String(localized: "tutorial.groups.2.body", defaultValue: "Open a person’s profile, tap Add to group, and select a group. A person can belong to more than one.", table: "Tutorials")),
            (String(localized: "tutorial.groups.3.title", defaultValue: "Your circles, your way", table: "Tutorials"),
             String(localized: "tutorial.groups.3.body", defaultValue: "Use the plus button in Groups to create your own circle, then choose its members.", table: "Tutorials"))
        ]
        }
    }
}

struct TutorialView: View {
    let topic: TutorialTopic
    let onAction: () -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var page = 0
    private var palette: WarmPalette { WarmTheme.palette(for: .subtle) }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // A vertical layout at accessibility sizes keeps long translations
                // readable without trapping a VoiceOver user in a page container.
                if typeSize.isAccessibilitySize {
                    pageContent(page)
                } else {
                    TabView(selection: $page) {
                        ForEach(topic.pages.indices, id: \.self) { index in
                            pageContent(index).tag(index)
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                }
                controls
            }
            .background(palette.paper.ignoresSafeArea())
            .navigationTitle(topic.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .accessibilityLabel(String(localized: "tutorial.close", defaultValue: "Close tutorial", table: "Tutorials"))
                    .accessibilityIdentifier("tutorial.close")
                }
            }
        }
        .tint(.indigo)
    }

    private func pageContent(_ index: Int) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text(topic.pages[index].title)
                    .font(WarmHeadingFont.font(size: 34, warmth: .subtle))
                    .foregroundStyle(palette.ink)
                    .accessibilityAddTraits(.isHeader)
                Text(topic.pages[index].body)
                    .font(.body)
                    .foregroundStyle(palette.ink2)
                    .fixedSize(horizontal: false, vertical: true)
                TutorialExample(topic: topic, page: index)
                    .padding(.vertical, 8)
                Text(String(localized: "tutorial.sample", defaultValue: "Example only · Sample contacts", table: "Tutorials"))
                    .font(.caption)
                    .foregroundStyle(palette.ink2)
            }
            .frame(maxWidth: 480, alignment: .leading)
            .padding(24)
            .frame(maxWidth: .infinity)
        }
    }

    private var controls: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                ForEach(topic.pages.indices, id: \.self) { index in
                    Capsule().fill(index == page ? Color.indigo : palette.ink3.opacity(0.3))
                        .frame(width: index == page ? 22 : 6, height: 6)
                }
            }
            .accessibilityHidden(true)
            Button {
                if page < topic.pages.count - 1 { page += 1 }
                else { onAction(); dismiss() }
            } label: {
                Text(page == topic.pages.count - 1 ? topic.actionTitle : String(localized: "tutorial.next", defaultValue: "Next", table: "Tutorials"))
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("tutorial.next")
            HStack {
                Button(String(localized: "tutorial.back", defaultValue: "Back", table: "Tutorials")) { page -= 1 }
                    .accessibilityIdentifier("tutorial.back")
                    .disabled(page == 0)
                    .opacity(page == 0 ? 0 : 1)
                    .frame(minHeight: 44)
                Spacer()
                Text(String(format: String(localized: "tutorial.progress", defaultValue: "%1$lld of %2$lld", table: "Tutorials"), page + 1, topic.pages.count))
                    .font(.subheadline)
                    .foregroundStyle(palette.ink2)
                    .accessibilityIdentifier("tutorial.progress")
            }
        }
        .frame(maxWidth: 480)
        .padding(.horizontal, 24)
        .padding(.bottom, 12)
        .frame(maxWidth: .infinity)
        .background(palette.paper)
    }
}

/// Lightweight recreations of the reviewed iOS surfaces. These use the same
/// warm tokens and system typography; fields are deliberately noninteractive.
private struct TutorialExample: View {
    let topic: TutorialTopic
    let page: Int
    @Environment(\.colorScheme) private var colorScheme
    private var palette: WarmPalette { WarmTheme.palette(for: .subtle) }
    private var blue: Color {
        colorScheme == .dark ? WarmCategory.friends.palette.accentSoft : WarmCategory.friends.palette.accent
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            switch (topic, page) {
            case (.tickle, 0):
                VStack(spacing: 16) {
                    MonogramAvatar(initials: "AN", category: .friends, size: 80)
                    Text(verbatim: "Alice Nguyen")
                        .font(WarmHeadingFont.font(size: 28, warmth: .subtle))
                    ViewThatFits(in: .horizontal) {
                        HStack { chip("message.fill", String(localized: "warm.contact.sendText", defaultValue: "Send a text"), filled: true); tickleChip; chip("phone.fill", String(localized: "tutorial.call", defaultValue: "Call", table: "Tutorials")) }
                        tickleChip
                    }
                }.frame(maxWidth: .infinity)
            case (.tickle, 1):
                VStack(alignment: .leading, spacing: 12) {
                    Text(String(localized: "tickleEdit.navTitle.new", defaultValue: "New Tickle")).font(.headline)
                    row(String(localized: "tickleEdit.section.who"), "Alice Nguyen")
                    row(String(localized: "tickleEdit.row.frequency"), String(localized: "tutorial.annual", defaultValue: "Annual", table: "Tutorials"))
                    row(String(localized: "tickleEdit.row.starting"), String(localized: "tutorial.exampleDate", defaultValue: "June 10", table: "Tutorials"))
                    Text(String(localized: "tickleEdit.section.commonAnnualEvents", defaultValue: "Common annual events")).font(.caption).foregroundStyle(.secondary)
                    Text(String(localized: "tickleEdit.preset.birthday", defaultValue: "Birthday"))
                        .padding(10).background(Color.indigo.opacity(0.12), in: Capsule()).foregroundStyle(.indigo)
                }
                .padding(16).background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
                .padding(12).background(Color(uiColor: .systemGroupedBackground), in: RoundedRectangle(cornerRadius: 22))
            case (.tickle, _):
                person("Alice Nguyen", initials: "AN")
                Text(String(localized: "tutorial.exampleNote", defaultValue: "Ask how the new role is going.", table: "Tutorials"))
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading)
                    .background(palette.paperSurface, in: RoundedRectangle(cornerRadius: 12))
                row(String(localized: "tutorial.catchUp", defaultValue: "Catch up", table: "Tutorials"), String(localized: "tutorial.monthly", defaultValue: "Monthly", table: "Tutorials"))
            case (.phone, 1):
                row("Alice Nguyen", "✓")
                row("Marcus Thompson", "✓")
                row("Priya Patel", "○")
            case (.linkedin, 1), (.linkedin, 2):
                Label { Text(verbatim: "Connections.csv") } icon: { Image(systemName: "doc.text") }
                    .font(.headline).foregroundStyle(blue)
                Text(String(localized: "tutorial.csvFields", defaultValue: "First name · Last name · Company", table: "Tutorials"))
                    .font(.subheadline).foregroundStyle(palette.ink2)
                if page == 2 {
                    Label(topic.actionTitle, systemImage: "folder").foregroundStyle(.indigo)
                }
            case (.groups, 0):
                WarmCard(category: .family, variant: .row, showPrompt: false, contactsCount: 2)
                    .allowsHitTesting(false)
                WarmCard(category: .work, variant: .row, showPrompt: false, contactsCount: 2)
                    .allowsHitTesting(false)
            case (.groups, 1):
                person("Alice Nguyen", initials: "AN")
                Label(String(localized: "contactDetail.button.addToGroup"), systemImage: "person.3")
                    .foregroundStyle(blue)
                row(WarmCategory.work.localizedGroupName, "✓")
            case (.groups, _):
                Label(String(localized: "tutorial.runningClub", defaultValue: "Running club", table: "Tutorials"), systemImage: "person.3.fill")
                    .font(.headline).foregroundStyle(blue)
                person("Alice Nguyen", initials: "AN")
                person("Priya Patel", initials: "PP")
            default:
                person("Alice Nguyen", initials: "AN")
                Divider()
                person("Marcus Thompson", initials: "MT")
            }
        }
        .font(.body)
        .foregroundStyle(palette.ink)
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.cardBg, in: RoundedRectangle(cornerRadius: WarmRadius.card))
        .overlay(RoundedRectangle(cornerRadius: WarmRadius.card).stroke(palette.cardBorder))
        .accessibilityElement(children: .combine)
    }

    private var tickleChip: some View {
        chip("bell.fill", String(localized: "warm.contact.createTickle", defaultValue: "Create a tickle"))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(blue, lineWidth: 2))
    }
    private func chip(_ symbol: String, _ title: String, filled: Bool = false) -> some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
            Text(title).font(.caption).multilineTextAlignment(.center)
        }
        .foregroundStyle(filled ? palette.cardBg : blue)
        .padding(10)
        .frame(maxWidth: .infinity, minHeight: 86)
        .background(filled ? blue : palette.paperSurfaceAlt, in: RoundedRectangle(cornerRadius: 12))
    }
    private func person(_ name: String, initials: String) -> some View {
        HStack(spacing: 12) {
            MonogramAvatar(initials: initials, category: .friends, size: 40)
            Text(verbatim: name).font(.body.weight(.medium))
        }
    }
    private func row(_ title: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(verbatim: title)
            Spacer(minLength: 12)
            Text(verbatim: value).foregroundStyle(blue)
        }
        .padding(.vertical, 6)
    }
}

/// The follow-up opens only after the guide finishes dismissing. Closing or
/// swiping away a guide records dismissal but never runs its final action.
private struct TutorialSheetModifier: ViewModifier {
    @Binding var topic: TutorialTopic?
    var onAction: (TutorialTopic) -> Void
    @State private var presentedTopic: TutorialTopic?
    @State private var pendingAction: TutorialTopic?

    func body(content: Content) -> some View {
        content.sheet(item: $topic, onDismiss: {
            if let presentedTopic {
                UserDefaults.standard.set(true, forKey: presentedTopic.dismissalKey)
            }
            if let pendingAction { onAction(pendingAction) }
            presentedTopic = nil
            pendingAction = nil
        }) { guide in
            TutorialView(topic: guide) { pendingAction = guide }
                .onAppear { presentedTopic = guide }
        }
    }
}

extension View {
    func tutorialSheet(topic: Binding<TutorialTopic?>, onAction: @escaping (TutorialTopic) -> Void) -> some View {
        modifier(TutorialSheetModifier(topic: topic, onAction: onAction))
    }
}

struct TutorialInvitation: View {
    let topic: TutorialTopic
    let onOpen: () -> Void
    @AppStorage private var dismissed: Bool
    @Environment(\.colorScheme) private var colorScheme
    private var palette: WarmPalette { WarmTheme.palette(for: .subtle) }

    init(topic: TutorialTopic, onOpen: @escaping () -> Void) {
        self.topic = topic
        self.onOpen = onOpen
        _dismissed = AppStorage(wrappedValue: false, topic.dismissalKey)
    }

    var body: some View {
        if !dismissed {
            HStack(spacing: 8) {
                Button(action: onOpen) {
                    Label(topic.title, systemImage: "questionmark.circle")
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 12)
                }
                .accessibilityIdentifier("tutorial.invitation.\(topic.rawValue)")
                Button { dismissed = true } label: {
                    Image(systemName: "xmark").frame(minWidth: 44, minHeight: 44)
                }
                .accessibilityLabel(String(localized: "tutorial.dismissInvitation", defaultValue: "Dismiss tutorial suggestion", table: "Tutorials"))
            }
            .foregroundStyle(colorScheme == .dark ? WarmCategory.friends.palette.accentSoft : WarmCategory.friends.palette.accent)
            .padding(.leading, 14)
            .background(palette.cardBg, in: RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct TutorialLibraryView: View {
    @State private var topic: TutorialTopic?
    @State private var destination: TutorialTopic?

    var body: some View {
        List(TutorialTopic.allCases) { guide in
            Button { topic = guide } label: {
                Label(guide.title, systemImage: guide.symbol)
                    .padding(.vertical, 8)
            }
            .accessibilityIdentifier("tutorial.library.\(guide.rawValue)")
        }
        .navigationTitle(String(localized: "tutorial.library", defaultValue: "Tutorials", table: "Tutorials"))
        .tutorialSheet(topic: $topic) { destination = $0 }
        .sheet(item: $destination) { guide in
            switch guide {
            case .phone: ImportView()
            case .linkedin: ImportView(initialAction: .linkedin)
            case .tickle: TickleEditView()
            case .groups:
                GroupListView()
                    .safeAreaInset(edge: .top) {
                        HStack {
                            Spacer()
                            Button(String(localized: "common.done")) { destination = nil }
                                .padding()
                        }
                        .background(WarmTheme.palette(for: .subtle).paper)
                    }
            }
        }
    }
}
