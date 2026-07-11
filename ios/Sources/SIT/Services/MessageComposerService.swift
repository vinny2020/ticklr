import SwiftUI
import MessageUI

/// UIViewControllerRepresentable wrapper around MFMessageComposeViewController.
/// Pre-populates recipients (phone numbers) and optional message body.
struct MessageComposerView: UIViewControllerRepresentable {
    let recipients: [String]
    let body: String
    /// Called after the composer dismisses, with the user's outcome
    /// (.sent / .cancelled / .failed) so the presenter can route accordingly.
    var onFinish: ((MessageComposeResult) -> Void)? = nil
    @Environment(\.dismiss) private var dismiss

    func makeCoordinator() -> Coordinator {
        Coordinator(dismiss: dismiss, onFinish: onFinish)
    }

    func makeUIViewController(context: Context) -> MFMessageComposeViewController {
        let vc = MFMessageComposeViewController()
        vc.recipients = recipients
        vc.body = body
        vc.messageComposeDelegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ uiViewController: MFMessageComposeViewController, context: Context) {}

    static func canSendMessages() -> Bool {
        #if DEBUG
        // Screenshot aid only: the Simulator's MFMessageComposeViewController
        // always reports canSendText() == false, so messaging actions never
        // appear. Launch with `-debugForceCanSendMessages YES` to force-show
        // them (matches a real iMessage-enabled iPad) for App Store captures.
        // Off by default; never compiled into release builds.
        if UserDefaults.standard.bool(forKey: "debugForceCanSendMessages") { return true }
        #endif
        return MFMessageComposeViewController.canSendText()
    }

    /// Shared gate for any "compose a text" affordance (TIC-93): true only
    /// when the target has at least one phone number to text AND the device
    /// can actually send text (false on iPad/Simulator/devices without
    /// Messages configured). Previously `TickleActionSheet`'s "Compose
    /// message" row checked both conditions while `ContactDetailView`'s
    /// "Send a text" chip only checked for a phone number — the chip could
    /// show enabled on a device that can't actually send. Both now call this.
    static func canCompose(phoneNumbers: [String]) -> Bool {
        !phoneNumbers.isEmpty && canSendMessages()
    }

    // MARK: - Coordinator
    class Coordinator: NSObject, @preconcurrency MFMessageComposeViewControllerDelegate {
        let dismiss: DismissAction
        let onFinish: ((MessageComposeResult) -> Void)?

        init(dismiss: DismissAction, onFinish: ((MessageComposeResult) -> Void)?) {
            self.dismiss = dismiss
            self.onFinish = onFinish
        }

        @MainActor
        func messageComposeViewController(
            _ controller: MFMessageComposeViewController,
            didFinishWith result: MessageComposeResult
        ) {
            onFinish?(result)
            dismiss()
        }
    }
}
