import SwiftUI

#if DEBUG
/// Deterministic trait transitions for UI regression tests. This supplements
/// physical resize/fold testing; it does not claim to simulate Duo posture.
@MainActor @Observable
private final class AdaptiveResizeTestState {
    static let shared = AdaptiveResizeTestState()
    var sizeClass: UserInterfaceSizeClass = .compact
}

private struct AdaptiveResizeTestHost: ViewModifier {
    @State private var state = AdaptiveResizeTestState.shared

    func body(content: Content) -> some View {
        if ProcessInfo.processInfo.arguments.contains("-debugTestAdaptiveResize") {
            content
                .environment(\.horizontalSizeClass, state.sizeClass)
                .overlay(alignment: .bottomTrailing) {
                    Button {
                        state.sizeClass = state.sizeClass == .compact ? .regular : .compact
                    } label: {
                        Image(systemName: "arrow.left.and.right")
                            .padding(12)
                            .background(.regularMaterial, in: Circle())
                    }
                    .accessibilityIdentifier("test.resize")
                    .accessibilityValue(state.sizeClass == .compact ? "compact" : "regular")
                    .padding(.bottom, 60)
                }
        } else {
            content
        }
    }
}
#endif

extension View {
    /// Compiles to the original view in release builds. Also applied to modal
    /// roots so a test can change traits while an editor covers the tab view.
    @ViewBuilder
    func adaptiveResizeTestHost() -> some View {
        #if DEBUG
        modifier(AdaptiveResizeTestHost())
        #else
        self
        #endif
    }
}
