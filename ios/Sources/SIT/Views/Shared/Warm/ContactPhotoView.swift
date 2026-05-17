import SwiftUI
import UIKit

/// Resolves a contact's avatar through this priority chain:
///   1. user-attached local photo (`PhotoStore`)
///   2. system Contacts photo (`ContactPhotoFetcher`)
///   3. monogram (small) or full empty-state photo affordance (large)
///
/// Spec: HANDOFF.md lines 8-49.
///
/// The empty-state monogram + dashed circle + camera badge + "Add a
/// photo" link is only rendered when `style == .detail`. List rows pass
/// `.list` for the compact tinted-monogram look.
struct ContactPhotoView: View {
    enum Style {
        case list   // 36 default — tinted monogram only
        case detail // 132 default — dashed empty affordance + camera badge
    }

    let contact: Contact
    let category: WarmCategory
    var style: Style = .list
    var size: CGFloat? = nil

    @State private var resolved: ResolutionState = .loading

    var body: some View {
        let resolvedSize = size ?? defaultSize
        ZStack {
            switch resolved {
            case .loading:
                Circle()
                    .fill(category.palette.accentTint)
                    .frame(width: resolvedSize, height: resolvedSize)
            case .image(let img):
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: resolvedSize, height: resolvedSize)
                    .clipShape(Circle())
            case .empty:
                emptyAffordance(size: resolvedSize)
            }
        }
        .task(id: contact.id) { await resolve() }
    }

    @ViewBuilder
    private func emptyAffordance(size: CGFloat) -> some View {
        switch style {
        case .list:
            MonogramAvatar(initials: contact.initials,
                           category: category,
                           size: size)
        case .detail:
            MonogramPhotoAffordance(initials: contact.initials,
                                    category: category,
                                    size: size)
        }
    }

    private var defaultSize: CGFloat {
        switch style {
        case .list:   36
        case .detail: 132
        }
    }

    @MainActor
    private func resolve() async {
        // Extract Sendable primitives before any actor hop — SwiftData
        // models are not Sendable.
        let id = contact.id
        let phones = contact.phoneNumbers
        let emails = contact.emails

        if let local = PhotoStore.localImage(for: id) {
            resolved = .image(local)
            return
        }
        if let system = await ContactPhotoFetcher.fetch(
            contactId: id, phoneNumbers: phones, emails: emails
        ) {
            resolved = .image(system)
            return
        }
        resolved = .empty
    }

    private enum ResolutionState {
        case loading
        case image(UIImage)
        case empty
    }
}
