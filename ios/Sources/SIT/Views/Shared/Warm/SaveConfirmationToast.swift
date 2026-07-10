import SwiftUI

/// Bottom toast for a plain save/update confirmation (TIC-84). Bound to an
/// optional message string — non-nil presents the toast; the auto-dismiss
/// timeout clears it. Unlike `TickleCompletionToast` this carries no Undo
/// affordance, since a sheet save isn't an undoable action — it's shown on
/// the surface that *remains* after a sheet (TickleEditView, TemplateEditView)
/// dismisses, since the sheet itself no longer exists to host it.
struct SaveConfirmationToast: ViewModifier {
    @Binding var message: String?
    var warmth: Warmth = .subtle

    @State private var dismissTask: Task<Void, Never>?

    private var isShowing: Bool { message != nil }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if isShowing, let message {
                    WarmToastCapsule(icon: "checkmark.circle.fill", text: message, warmth: warmth) {
                        EmptyView()
                    }
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
                    try? await Task.sleep(for: .seconds(2.5))
                    if !Task.isCancelled { message = nil }
                }
            }
    }
}

extension View {
    /// Presents a save/update confirmation toast bound to `message` — set the
    /// message to show it, `nil` hides it. Meant to be hosted on the presenter
    /// that remains after a sheet dismisses (TIC-84), not on the sheet itself.
    func saveConfirmationToast(message: Binding<String?>, warmth: Warmth = .subtle) -> some View {
        modifier(SaveConfirmationToast(message: message, warmth: warmth))
    }
}
