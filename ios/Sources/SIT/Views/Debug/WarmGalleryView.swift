#if DEBUG
import SwiftUI
import SwiftData
import PhotosUI

/// Debug-only visual gallery of every warm primitive in isolation.
/// Wired into Settings → Debug → "Warm Gallery". Helps eyeball component
/// changes without rewriting full screens. NOT in release builds.
struct WarmGalleryView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Contact.firstName) private var contacts: [Contact]

    @State private var warmth: Warmth = .subtle
    @State private var activeFilter: WarmCategory? = nil
    @State private var photoDemoCategory: WarmCategory = .family
    @State private var pickerSelection: PhotosPickerItem? = nil
    @State private var pickerError: String? = nil
    @State private var photoRefreshKey = UUID()

    var body: some View {
        let palette = WarmTheme.palette(for: warmth)
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                warmthPicker

                section("Eyebrows") {
                    VStack(alignment: .leading, spacing: 12) {
                        WarmEyebrow(text: "Today", warmth: warmth)
                        WarmEyebrow(text: "Upcoming", warmth: warmth)
                        WarmEyebrow(text: "All contacts", warmth: warmth)
                    }
                }

                section("Category badges") {
                    HStack(spacing: 12) {
                        ForEach(WarmCategory.allCases) { cat in
                            CategoryBadge(category: cat)
                        }
                    }
                }

                section("Monogram avatars (list size)") {
                    HStack(spacing: 16) {
                        ForEach(WarmCategory.allCases) { cat in
                            MonogramAvatar(initials: "AB", category: cat)
                        }
                    }
                }

                section("Photo affordance (empty state)") {
                    HStack(spacing: 24) {
                        ForEach([WarmCategory.family, .friends, .work]) { cat in
                            VStack(spacing: 8) {
                                MonogramPhotoAffordance(initials: "JS", category: cat)
                                Text(cat.localizedLabel)
                                    .font(.caption2)
                                    .foregroundStyle(palette.ink2)
                            }
                        }
                    }
                }

                section("Photo resolver (live)") {
                    photoResolverDemo
                }

                section("Filter chips — inactive") {
                    scrollableChipRow { chipRow(active: false) }
                }

                section("Filter chips — active") {
                    scrollableChipRow { chipRow(active: true) }
                }

                section("Filter chips — interactive") {
                    scrollableChipRow {
                        HStack(spacing: 8) {
                            WarmFilterChip(
                                kind: .all,
                                label: "All",
                                count: 142,
                                isActive: activeFilter == nil
                            ) { activeFilter = nil }
                            ForEach([WarmCategory.family, .friends, .work, .community]) { cat in
                                WarmFilterChip(
                                    kind: .category(cat),
                                    label: cat.localizedLabel,
                                    count: 12,
                                    isActive: activeFilter == cat
                                ) { activeFilter = cat }
                            }
                        }
                    }
                }

                section("Tickle prompt strips") {
                    VStack(spacing: 10) {
                        ForEach(WarmCategory.allCases) { cat in
                            TicklePrompt(category: cat, warmth: warmth)
                        }
                    }
                }

                section("Illustrations (16:9)") {
                    VStack(spacing: 10) {
                        ForEach(WarmCategory.allCases) { cat in
                            WarmIllustration(category: cat)
                                .aspectRatio(16.0 / 9.0, contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .overlay(alignment: .topLeading) {
                                    Text(cat.localizedLabel.uppercased())
                                        .font(.system(size: 9, weight: .semibold))
                                        .tracking(1.2)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 3)
                                        .background(.thinMaterial)
                                        .clipShape(Capsule())
                                        .padding(8)
                                }
                        }
                    }
                }

                section("Hero card (Family)") {
                    WarmCard(category: .family, variant: .hero, warmth: warmth)
                }

                section("Compact cards") {
                    VStack(spacing: 12) {
                        ForEach(WarmCategory.allCases) { cat in
                            WarmCard(
                                category: cat,
                                variant: .compact,
                                warmth: warmth,
                                showPrompt: false,
                                contactsCount: 8
                            )
                        }
                    }
                }

                section("Row cards (Groups list)") {
                    VStack(spacing: 12) {
                        ForEach(WarmCategory.allCases) { cat in
                            WarmCard(
                                category: cat,
                                variant: .row,
                                warmth: warmth,
                                showPrompt: true,
                                contactsCount: 12
                            )
                        }
                    }
                }

                section("Warm list container") {
                    WarmListContainer(warmth: warmth) {
                        ForEach(0..<3) { i in
                            HStack(spacing: 12) {
                                MonogramAvatar(
                                    initials: ["AB", "CD", "EF"][i],
                                    category: WarmCategory.allCases[i]
                                )
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(["Anna Brown", "Carla Diaz", "Eli Foster"][i])
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(palette.ink)
                                    Text(WarmCategory.allCases[i].localizedLabel + " · last contacted 3d ago")
                                        .font(.caption)
                                        .foregroundStyle(palette.ink2)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(palette.ink3)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            if i < 2 { WarmRowDivider(warmth: warmth) }
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(palette.paper.ignoresSafeArea())
        .navigationTitle("Warm Gallery")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            WarmEyebrow(text: title, warmth: warmth)
            content()
        }
    }

    private var warmthPicker: some View {
        Picker("Warmth tier", selection: $warmth) {
            ForEach(Warmth.allCases, id: \.self) { w in
                Text(w.rawValue.capitalized).tag(w)
            }
        }
        .pickerStyle(.segmented)
    }

    private func chipRow(active: Bool) -> some View {
        HStack(spacing: 8) {
            WarmFilterChip(kind: .all, label: "All", count: 142, isActive: active) {}
            ForEach([WarmCategory.family, .friends, .work, .community]) { cat in
                WarmFilterChip(
                    kind: .category(cat),
                    label: cat.localizedLabel,
                    count: 12,
                    isActive: active
                ) {}
            }
        }
    }

    // MARK: - Photo resolver demo

    @ViewBuilder
    private var photoResolverDemo: some View {
        let palette = WarmTheme.palette(for: warmth)
        if let contact = contacts.first {
            VStack(alignment: .leading, spacing: 14) {
                Text("Resolving for: \(contact.fullName.isEmpty ? "(unnamed)" : contact.fullName)")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(palette.ink)
                Text("Priority: local photo → system Contacts photo → monogram fallback")
                    .font(.caption2)
                    .foregroundStyle(palette.ink2)

                HStack(alignment: .top, spacing: 24) {
                    VStack(spacing: 6) {
                        ContactPhotoView(contact: contact, category: photoDemoCategory, style: .list)
                            .id(photoRefreshKey)
                        Text("List (36)").font(.caption2).foregroundStyle(palette.ink2)
                    }
                    VStack(spacing: 6) {
                        ContactPhotoView(contact: contact, category: photoDemoCategory, style: .detail)
                            .id(photoRefreshKey)
                        Text("Detail (132)").font(.caption2).foregroundStyle(palette.ink2)
                    }
                }

                Picker("Category accent", selection: $photoDemoCategory) {
                    ForEach(WarmCategory.allCases) { cat in
                        Text(cat.localizedLabel).tag(cat)
                    }
                }
                .pickerStyle(.menu)

                HStack(spacing: 12) {
                    PhotosPicker(selection: $pickerSelection,
                                 matching: .images,
                                 photoLibrary: .shared()) {
                        Label("Attach local photo", systemImage: "photo.badge.plus")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    Button(role: .destructive) {
                        PhotoStore.delete(for: contact.id)
                        ContactPhotoFetcher.clearCache()
                        photoRefreshKey = UUID()
                    } label: {
                        Label("Remove local photo", systemImage: "trash")
                            .font(.system(size: 13, weight: .semibold))
                    }
                }
                if let err = pickerError {
                    Text(err).font(.caption2).foregroundStyle(.red)
                }
            }
            .onChange(of: pickerSelection) { _, newItem in
                guard let newItem else { return }
                Task {
                    do {
                        guard let data = try await newItem.loadTransferable(type: Data.self),
                              let img = UIImage(data: data) else {
                            pickerError = "Couldn't decode the selected image."
                            return
                        }
                        try PhotoStore.save(img, for: contact.id)
                        ContactPhotoFetcher.clearCache()
                        await MainActor.run {
                            photoRefreshKey = UUID()
                            pickerError = nil
                        }
                    } catch {
                        pickerError = "Save failed: \(error.localizedDescription)"
                    }
                }
            }
        } else {
            Text("No contacts to resolve. Use Debug → Load Test Contacts.")
                .font(.caption)
                .foregroundStyle(palette.ink2)
        }
    }

    @ViewBuilder
    private func scrollableChipRow<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            content()
                .padding(.horizontal, 1)  // keeps capsule borders from clipping
        }
        // Cancel the parent's horizontal padding so the row bleeds full-width
        // and feels truly sticky-style when scrolled.
        .padding(.horizontal, -20)
        .safeAreaPadding(.horizontal, 20)
    }
}

#Preview {
    NavigationStack {
        WarmGalleryView()
    }
}
#endif
