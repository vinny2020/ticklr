import SwiftUI
import SwiftData

struct NetworkListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.lastName) private var contacts: [Contact]

    @State private var searchText = ""
    @State private var filter: Filter = .all
    @State private var showingImport = false
    @State private var showingAddContact = false

    private let warmth: Warmth = .subtle
    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }

    /// Filter taxonomy from HANDOFF lines 56-58: All + 4 chips, with
    /// Milestones intentionally excluded (it's a Tickle-tab concept).
    private enum Filter: Hashable {
        case all
        case category(WarmCategory)
    }

    private static let chipCategories: [WarmCategory] =
        [.family, .friends, .work, .community]

    var body: some View {
        NavigationStack {
            List {
                // MARK: Inline title + subtitle (scrolls away)
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(localized: "networkList.navTitle"))
                            .font(WarmHeadingFont.font(size: 32, warmth: warmth))
                            .tracking(WarmHeadingFont.tracking(warmth: warmth))
                            .foregroundStyle(palette.ink)
                        Text(String(localized: "warm.network.subtitle",
                                    defaultValue: "The people you keep in your circle."))
                            .font(.system(size: 14))
                            .foregroundStyle(palette.ink2)
                    }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                }

                // MARK: Filter chips (scroll with content)
                Section {
                    filterChipsRow
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 8, trailing: 0))
                }

                // MARK: Contacts
                Section {
                    ForEach(filtered) { contact in
                        ZStack {
                            NavigationLink {
                                ContactDetailView(contact: contact)
                            } label: {
                                EmptyView()
                            }
                            .opacity(0)

                            ContactRowView(contact: contact, warmth: warmth)
                        }
                        .listRowBackground(palette.cardBg)
                        .listRowSeparatorTint(palette.cardBorder)
                        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(palette.paper.ignoresSafeArea())
            .searchable(text: $searchText,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: String(localized: "networkList.search"))
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            showingAddContact = true
                        } label: {
                            Label(String(localized: "networkList.menu.newContact"),
                                  systemImage: "person.badge.plus")
                        }
                        Button {
                            showingImport = true
                        } label: {
                            Label(String(localized: "networkList.menu.getStarted"),
                                  systemImage: "square.and.arrow.down")
                        }
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(palette.ink)
                    }
                }
            }
            .overlay {
                if contacts.isEmpty {
                    ContentUnavailableView(
                        String(localized: "networkList.empty.title"),
                        systemImage: "person.2.slash",
                        description: Text(String(localized: "networkList.empty.description"))
                    )
                } else if !searchText.isEmpty && filtered.isEmpty {
                    ContentUnavailableView.search
                } else if filtered.isEmpty {
                    ContentUnavailableView(
                        String(localized: "warm.network.empty.filtered",
                               defaultValue: "No contacts in this group yet"),
                        systemImage: "person.2"
                    )
                }
            }
            .sheet(isPresented: $showingImport) { ImportView() }
            .sheet(isPresented: $showingAddContact) { AddContactView() }
        }
    }

    // MARK: - Filter chip row

    private var filterChipsRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                WarmFilterChip(
                    kind: .all,
                    label: String(localized: "warm.network.filter.all", defaultValue: "All"),
                    count: contacts.count,
                    isActive: filter == .all,
                    action: { filter = .all },
                    warmth: warmth
                )
                ForEach(Self.chipCategories, id: \.self) { cat in
                    let n = countFor(category: cat)
                    if n > 0 || filter == .category(cat) {
                        WarmFilterChip(
                            kind: .category(cat),
                            label: cat.localizedLabel,
                            count: n,
                            isActive: filter == .category(cat),
                            action: { filter = .category(cat) },
                            warmth: warmth
                        )
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Filtering

    private var filtered: [Contact] {
        var result = contacts

        switch filter {
        case .all:
            break
        case .category(let cat):
            let canonicalId = cat.groupUUID
            result = result.filter { c in
                c.groups.contains { $0.id == canonicalId }
            }
        }

        if !searchText.isEmpty {
            result = result.filter {
                $0.fullName.localizedCaseInsensitiveContains(searchText) ||
                $0.company.localizedCaseInsensitiveContains(searchText)
            }
        }

        return result
    }

    private func countFor(category: WarmCategory) -> Int {
        let canonicalId = category.groupUUID
        return contacts.reduce(0) { acc, c in
            acc + (c.groups.contains { $0.id == canonicalId } ? 1 : 0)
        }
    }
}
