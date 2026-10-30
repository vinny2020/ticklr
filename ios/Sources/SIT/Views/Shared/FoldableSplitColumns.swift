import SwiftUI

private struct FoldableSidebarWidthKey: EnvironmentKey {
    static let defaultValue: CGFloat? = nil
}

private extension EnvironmentValues {
    var foldableSidebarWidth: CGFloat? {
        get { self[FoldableSidebarWidthKey.self] }
        set { self[FoldableSidebarWidthKey.self] = newValue }
    }
}

private struct SplitGeometry: Equatable {
    var width: CGFloat
    var containsDivision: Bool
}

private struct FoldableSplitColumns: ViewModifier {
    @Binding var columnVisibility: NavigationSplitViewVisibility
    @State private var hasHinge = false
    @State private var containerWidth: CGFloat = 0

    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            styledContent(content)
                .environment(\.foldableSidebarWidth,
                             hasHinge && containerWidth > 0 ? containerWidth / 2 : nil)
                .onGeometryChange(for: SplitGeometry.self) { geometry in
                    SplitGeometry(
                        width: geometry.size.width,
                        containsDivision: !geometry.reservedRegions(
                            kind: .division, options: .includeInactive
                        ).isEmpty
                    )
                } action: { geometry in
                    if geometry.width > 0 { containerWidth = geometry.width }
                    // Keep the capability once discovered. The outer display
                    // has no division, but folding must not replace the sidebar
                    // subtree and its scroll position or presented draft.
                    hasHinge = hasHinge || geometry.containsDivision
                    if geometry.containsDivision { columnVisibility = .all }
                }
                .onHingeChange { _, context in
                    if let hinge = context.hinge {
                        hasHinge = true
                        if hinge.status != .closed { columnVisibility = .all }
                    }
                }
        } else {
            content
        }
    }

    @ViewBuilder
    private func styledContent(_ content: Content) -> some View {
        if hasHinge {
            content.navigationSplitViewStyle(.balanced)
        } else {
            content
        }
    }
}

private struct FoldableSplitColumn: ViewModifier {
    @Environment(\.foldableSidebarWidth) private var width
    var isSidebar: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if let width {
            content.navigationSplitViewColumnWidth(
                min: isSidebar ? width : 0,
                ideal: width,
                max: isSidebar ? width : nil
            )
        } else {
            // Leave the system's iPad column sizing untouched.
            content
        }
    }
}

extension View {
    /// Keep the two halves equally useful on a folding display, including when
    /// fully open and the system no longer treats its hinge as a division.
    func foldableSplitColumns(columnVisibility: Binding<NavigationSplitViewVisibility>) -> some View {
        modifier(FoldableSplitColumns(columnVisibility: columnVisibility))
    }

    func foldableSidebarColumn() -> some View {
        modifier(FoldableSplitColumn(isSidebar: true))
    }

    func foldableDetailColumn() -> some View {
        modifier(FoldableSplitColumn(isSidebar: false))
    }
}
