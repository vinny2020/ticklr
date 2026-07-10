import SwiftUI

/// Bottom toast shown after a sent text auto-completes a due tickle (TIC-82):
/// "Tickle marked done" with an Undo affordance. Bound to an optional
/// `CompletionSnapshot` — non-nil presents the toast; tapping Undo or the
/// auto-dismiss timeout clears it. The snapshot carries the exact pre-completion
/// state so `onUndo` can restore it.
struct TickleCompletionToast: ViewModifier {
    @Binding var snapshot: TickleScheduler.CompletionSnapshot?
    var warmth: Warmth = .subtle
    /// Runs when the user taps Undo, before the toast is cleared.
    let onUndo: () -> Void

    @State private var dismissTask: Task<Void, Never>?

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }
    /// Neutral accent for the checkmark + Undo affordance — matches ComposeView's
    /// no-contact fallback so the toast reads as part of the same flow.
    private var accent: Color { WarmCategory.community.palette.accent }
    private var isShowing: Bool { snapshot != nil }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if isShowing {
                    toast
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
                    try? await Task.sleep(for: .seconds(5))
                    if !Task.isCancelled { snapshot = nil }
                }
            }
    }

    private var toast: some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(accent)
            Text(String(localized: "tickle.completedToast.title",
                        defaultValue: "Tickle marked done"))
                .font(.subheadline)
                .foregroundStyle(palette.ink)
            Spacer(minLength: 8)
            Button(String(localized: "common.undo", defaultValue: "Undo")) {
                onUndo()
                snapshot = nil
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(accent)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(palette.cardBg)
        .clipShape(RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WarmRadius.surface, style: .continuous)
                .stroke(palette.cardBorder, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.12), radius: 8, y: 3)
    }
}

extension View {
    /// Presents the send-driven tickle-completion undo toast bound to `snapshot`.
    func tickleCompletionToast(
        snapshot: Binding<TickleScheduler.CompletionSnapshot?>,
        warmth: Warmth = .subtle,
        onUndo: @escaping () -> Void
    ) -> some View {
        modifier(TickleCompletionToast(snapshot: snapshot, warmth: warmth, onUndo: onUndo))
    }
}
