import SwiftUI
import Contacts
import UIKit

/// Tappable banner shown below the photo affordance on Contact Detail when
/// the app doesn't have Contacts access yet. Tap behavior depends on the
/// current `CNContactStore.authorizationStatus(for: .contacts)`:
///
///   - `.notDetermined`  → calls `requestAccess` (system prompt)
///   - `.denied` / `.restricted` → deep-links to system Settings
///   - `.authorized` / `.limited` (iOS 18+) → banner hides
///
/// Hidden when the contact has no phone numbers or emails (no system
/// match is possible regardless of permission).
///
/// When the user returns from Settings and the status changes to
/// authorized/limited, `onGranted` fires so the caller can invalidate
/// the photo cache and re-resolve.
struct ContactsAccessBanner: View {
    let contact: Contact
    let category: WarmCategory
    var warmth: Warmth = .subtle
    /// Fired when permission goes from missing to granted.
    var onGranted: () -> Void = {}

    @State private var status: CNAuthorizationStatus =
        CNContactStore.authorizationStatus(for: .contacts)
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        if shouldShow {
            Button(action: handleTap) {
                HStack(spacing: 8) {
                    Image(systemName: "person.crop.circle.badge.questionmark")
                        .font(.system(size: 14, weight: .semibold))
                    Text(String(
                        localized: "warm.contact.accessBanner",
                        defaultValue: "Allow contacts access to use the photo from your address book."
                    ))
                    .font(.system(size: 12))
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 6)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                }
                .foregroundStyle(category.palette.accent)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(category.palette.accentTint)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, WarmSpacing.lg)
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { reread() }
            }
        }
    }

    // MARK: - State logic

    private var hasMatchableContactInfo: Bool {
        !contact.phoneNumbers.isEmpty || !contact.emails.isEmpty
    }

    private var shouldShow: Bool {
        guard hasMatchableContactInfo else { return false }
        switch status {
        case .authorized: return false
        case .notDetermined, .denied, .restricted: return true
        @unknown default:
            // iOS 18+ `.limited` lands here on the iOS 17 SDK we currently
            // build against. Treat .limited (rawValue 4) as granted — the
            // user opted in for at least some contacts.
            return status.rawValue != 4
        }
    }

    private func handleTap() {
        switch status {
        case .notDetermined:
            Task { await requestAccess() }
        case .denied, .restricted:
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        default:
            break
        }
    }

    @MainActor
    private func requestAccess() async {
        do {
            let granted = try await CNContactStore().requestAccess(for: .contacts)
            let new = CNContactStore.authorizationStatus(for: .contacts)
            status = new
            if granted { onGranted() }
        } catch {
            status = CNContactStore.authorizationStatus(for: .contacts)
        }
    }

    private func reread() {
        let old = status
        let new = CNContactStore.authorizationStatus(for: .contacts)
        status = new
        // If we just gained access, refresh photos.
        let wasMissing = (old != .authorized && old.rawValue != 4)
        let nowGranted = (new == .authorized || new.rawValue == 4)
        if wasMissing && nowGranted { onGranted() }
    }
}
