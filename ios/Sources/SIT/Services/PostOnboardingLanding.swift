import Foundation

/// One-shot signal for where the app should land the moment onboarding
/// completes via a *success* path — a phone/CSV import or "Add my first
/// contact" (TIC-85). This is deliberately narrower than the app's general
/// default tab (still `.tickle`, unchanged here — see TIC-93): it only fires
/// once, for the specific transition out of onboarding, so the user sees the
/// network they just built instead of an empty Tickle tab. The "Skip for
/// now" path never calls into this, so it keeps landing on Tickle as before.
///
/// Backed directly by `UserDefaults` rather than an in-memory singleton
/// (contrast `TickleNotificationCoordinator`): `SITApp`'s root view discards
/// `OnboardingView` and mounts a brand-new `ContentView` the instant
/// `hasCompletedOnboarding` flips true, so there is no shared object graph
/// alive across that transition to publish through — but `UserDefaults`
/// trivially survives it. Only plain values (`Bool`/`String`) ever pass
/// through here, never `@Model` objects.
///
/// Consumed exactly once each: `ContentView.onAppear` reads + clears the
/// landing request to pick the initial tab; `NetworkListView.onAppear` reads
/// + clears the optional toast the same way. A later ordinary visit to
/// either screen finds nothing pending and behaves normally.
enum PostOnboardingLanding {
    private static let requestedKey = "pendingPostOnboardingLanding"
    private static let toastKey = "pendingPostOnboardingToast"

    /// Requests that the next `ContentView` land on the Network tab, and
    /// optionally hands `NetworkListView` a toast message to surface once it
    /// appears (e.g. "Imported 12 contacts · 3 duplicates skipped").
    static func requestNetworkLanding(toast: String? = nil) {
        UserDefaults.standard.set(true, forKey: requestedKey)
        if let toast, !toast.isEmpty {
            UserDefaults.standard.set(toast, forKey: toastKey)
        }
    }

    /// Reads and clears the pending landing request.
    @discardableResult
    static func consumeLandingRequest() -> Bool {
        defer { UserDefaults.standard.removeObject(forKey: requestedKey) }
        return UserDefaults.standard.bool(forKey: requestedKey)
    }

    /// Reads and clears the pending toast message, if one was attached.
    static func consumeToast() -> String? {
        defer { UserDefaults.standard.removeObject(forKey: toastKey) }
        return UserDefaults.standard.string(forKey: toastKey)
    }
}
