import SwiftUI
import MessageUI

/// UIViewControllerRepresentable wrapper around MFMessageComposeViewController.
/// Pre-populates recipients (phone numbers) and optional message body.
struct MessageComposerView: UIViewControllerRepresentable {
    let recipients: [String]
    let body: String
    @Environment(\.dismiss) private var dismiss

    func makeCoordinator() -> Coordinator {
        Coordinator(dismiss: dismiss)
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

    // MARK: - Coordinator
    class Coordinator: NSObject, @preconcurrency MFMessageComposeViewControllerDelegate {
        let dismiss: DismissAction

        init(dismiss: DismissAction) {
            self.dismiss = dismiss
        }

        @MainActor
        func messageComposeViewController(
            _ controller: MFMessageComposeViewController,
            didFinishWith result: MessageComposeResult
        ) {
            dismiss()
        }
    }
}
