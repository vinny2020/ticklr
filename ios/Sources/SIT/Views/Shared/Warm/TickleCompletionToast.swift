import SwiftUI

/// Shared bottom-toast pill: icon + message + optional trailing affordance.
/// Extracted so `TickleCompletionToast` (send-completion undo, TIC-82) and
/// `SaveConfirmationToast` (sheet-save confirmation, TIC-84) render identical
/// chrome without duplicating the background/border/shadow styling.
struct WarmToastCapsule<Trailing: View>: View {
    let icon: String
    let text: String
    var warmth: Warmth = .subtle
    @ViewBuilder var trailing: Trailing

    private var palette: WarmPalette { WarmTheme.palette(for: warmth) }
    /// Neutral accent for the checkmark — matches ComposeView's no-contact
    /// fallback so every toast reads as part of the same flow.
    private var accent: Color { WarmCategory.community.palette.accent }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(accent)
            Text(verbatim: text)
                .font(.subheadline)
                .foregroundStyle(palette.ink)
            Spacer(minLength: 8)
            trailing
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

/// Bottom toast bound to an optional `CompletionSnapshot`: non-nil presents an
/// `icon + text + Undo` capsule; tapping Undo or the auto-dismiss timeout clears
/// it. The snapshot carries the exact pre-action state so `onUndo` can restore it.
/// Shared chrome/timer behind both the mark-done toast ("Tickle marked done",
/// TIC-82) and the snooze toast ("Tickle snoozed", TIC-87) — see the two
/// `View` conveniences below. `CompletionSnapshot` doubles as the snooze snapshot
/// (its fields fully capture a snooze).
struct SnapshotUndoToast: ViewModifier {
    @Binding var snapshot: TickleScheduler.CompletionSnapshot?
    let icon: String
    let text: String
    var warmth: Warmth = .subtle
    /// Runs when the user taps Undo, before the toast is cleared.
    let onUndo: () -> Void

    @State private var dismissTask: Task<Void, Never>?

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
        WarmToastCapsule(icon: icon, text: text, warmth: warmth) {
            Button(String(localized: "common.undo", defaultValue: "Undo")) {
                onUndo()
                snapshot = nil
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(accent)
        }
    }
}

extension View {
    /// Presents the tickle mark-done undo toast bound to `snapshot` — set by the
    /// send-driven auto-completion (TIC-82) and every in-app mark-done surface
    /// (TIC-87: row checkmark, leading swipe, action sheet, iPad pane).
    func tickleCompletionToast(
        snapshot: Binding<TickleScheduler.CompletionSnapshot?>,
        warmth: Warmth = .subtle,
        onUndo: @escaping () -> Void
    ) -> some View {
        modifier(SnapshotUndoToast(
            snapshot: snapshot,
            icon: "checkmark.circle.fill",
            text: String(localized: "tickle.completedToast.title",
                         defaultValue: "Tickle marked done"),
            warmth: warmth,
            onUndo: onUndo
        ))
    }

    /// Presents the tickle snooze undo toast bound to `snapshot` (TIC-87): every
    /// snooze — swipe fast-path or action-sheet/iPad-pane duration choice — shows
    /// this, restoring the exact prior state via `undoSnooze`.
    func tickleSnoozeToast(
        snapshot: Binding<TickleScheduler.CompletionSnapshot?>,
        warmth: Warmth = .subtle,
        onUndo: @escaping () -> Void
    ) -> some View {
        modifier(SnapshotUndoToast(
            snapshot: snapshot,
            icon: "zzz",
            text: String(localized: "tickle.snoozedToast.title",
                         defaultValue: "Tickle snoozed"),
            warmth: warmth,
            onUndo: onUndo
        ))
    }
}
