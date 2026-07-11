import SwiftUI
import ContactsUI
import Contacts

/// `UIViewControllerRepresentable` wrapper around `CNContactPickerViewController`
/// configured for multi-select (TIC-93) — lets the user hand-pick specific
/// contacts to import instead of the existing all-at-once "Select iPhone
/// Contacts" sweep. Implementing the plural `contactPicker(_:didSelect
/// contacts:)` delegate method (rather than the singular `didSelect contact:`)
/// is what opts into Apple's built-in multi-select UI — checkmarked rows plus
/// a "Done" button — instead of single-tap-to-dismiss.
///
/// `CNContactPickerViewController` is a system picker: presenting it never
/// requires the app to hold Contacts authorization, and the `CNContact`
/// objects it hands back are already populated with the standard fields
/// (name, phone numbers, emails, organization, job title) this app reads —
/// no separate `CNContactStore` re-fetch needed.
///
/// The picker manages its own internal presentation lifecycle in a way that
/// doesn't play well with SwiftUI driving it purely via `.sheet(isPresented:)`,
/// so both delegate outcomes (selection and cancel) flip the `isPresented`
/// binding themselves — that binding is the single source of truth for
/// whether the sheet is up, matching the pattern SwiftUI expects.
struct ContactPickerRepresentable: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    /// Called with the user's selection once they tap "Done" with at least
    /// one contact checked. Not called on Cancel or an empty selection.
    let onSelect: ([CNContact]) -> Void

    func makeUIViewController(context: Context) -> CNContactPickerViewController {
        let picker = CNContactPickerViewController()
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: CNContactPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(isPresented: $isPresented, onSelect: onSelect)
    }

    final class Coordinator: NSObject, CNContactPickerDelegate {
        @Binding var isPresented: Bool
        let onSelect: ([CNContact]) -> Void

        init(isPresented: Binding<Bool>, onSelect: @escaping ([CNContact]) -> Void) {
            _isPresented = isPresented
            self.onSelect = onSelect
        }

        func contactPicker(_ picker: CNContactPickerViewController, didSelect contacts: [CNContact]) {
            isPresented = false
            guard !contacts.isEmpty else { return }
            onSelect(contacts)
        }

        func contactPickerDidCancel(_ picker: CNContactPickerViewController) {
            isPresented = false
        }
    }
}
