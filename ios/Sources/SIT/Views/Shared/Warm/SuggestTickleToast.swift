import SwiftUI

/// Bottom toast offering to create a tickle for the contact just texted, shown
/// after a plain (reminder-less) send (TIC-86): "Create a tickle for [name]?"
/// with a trailing Create affordance. Bound to an optional `TickleSuggestion` —
/// non-nil presents the toast; tapping Create or the auto-dismiss timeout clears
/// it. The suggestion carries only the contact's `id`+name, so `onCreate` can
/// re-fetch the live `Contact` on the surface that remains.
///
/// This is the create-a-tickle counterpart to `TickleCompletionToast` (TIC-82's
/// send-completion undo). The two are mutually exclusive for a single send — see
/// `ComposeSuggestions.postSendSurface`.
struct SuggestTickleToast: ViewModifier {
    @Binding var suggestion: TickleSuggestion?
    var warmth: Warmth = .subtle
    /// Runs when the user taps Create, before the toast is cleared.
    let onCreate: (TickleSuggestion) -> Void

    @State private var dismissTask: Task<Void, Never>?

    private var accent: Color { WarmCategory.community.palette.accent }
    private var isShowing: Bool { suggestion != nil }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let suggestion {
                    toast(for: suggestion)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: isShowing)
            .onChange(of: isShowing) { _, showing in
                dismissTask?.cancel()
                guard showing else { return }
                dismissTask = Task {
                    // Slightly longer than the plain save toast: this one is
                    // actionable, so give the user a beat to reach for it.
                    try? await Task.sleep(for: .seconds(6))
                    if !Task.isCancelled { suggestion = nil }
                }
            }
    }

    private func toast(for suggestion: TickleSuggestion) -> some View {
        WarmToastCapsule(
            icon: "bell.badge",
            text: String(localized: "compose.suggestTickle.offer \(suggestion.contactName)"),
            warmth: warmth
        ) {
            Button(String(localized: "common.create")) {
                onCreate(suggestion)
                self.suggestion = nil
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(accent)
        }
    }
}

extension View {
    /// Presents the create-a-tickle offer toast bound to `suggestion` (TIC-86).
    /// Hosted on the surface that remains after the compose surface dismisses.
    func suggestTickleToast(
        suggestion: Binding<TickleSuggestion?>,
        warmth: Warmth = .subtle,
        onCreate: @escaping (TickleSuggestion) -> Void
    ) -> some View {
        modifier(SuggestTickleToast(suggestion: suggestion, warmth: warmth, onCreate: onCreate))
    }
}
