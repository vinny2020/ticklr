import XCTest
@testable import Ticklr

/// TIC-93: `MessageComposerView.canCompose(phoneNumbers:)` is the single
/// shared gate for any "compose a text" affordance. Before this ticket,
/// `ContactDetailView`'s "Send a text" chip only checked for a non-empty
/// phone list, while `TickleActionSheet`'s "Compose message" row also
/// required `MessageComposerView.canSendMessages()` — the chip could show
/// enabled on a device that can't actually send text (Simulator, an iPad
/// with Messages not configured). Both call sites now share this one helper.
final class MessageComposerCanComposeTests: XCTestCase {

    override func tearDown() {
        UserDefaults.standard.removeObject(forKey: "debugForceCanSendMessages")
        super.tearDown()
    }

    func testEmptyPhoneNumbersNeverCanCompose() {
        // Force the device-capability half of the check to true so this
        // isolates the phone-number half — even a device that *can* send
        // text has nothing to text without a number.
        UserDefaults.standard.set(true, forKey: "debugForceCanSendMessages")
        XCTAssertFalse(
            MessageComposerView.canCompose(phoneNumbers: []),
            "no phone number to text — must be false regardless of device capability"
        )
    }

    func testNonEmptyPhoneNumbersRequiresDeviceCanSend() {
        UserDefaults.standard.set(false, forKey: "debugForceCanSendMessages")
        XCTAssertFalse(
            MessageComposerView.canCompose(phoneNumbers: ["555-0100"]),
            "a phone number alone isn't enough — the device must also be able to send text"
        )
    }

    func testNonEmptyPhoneNumbersAndSendCapableDeviceCanCompose() {
        UserDefaults.standard.set(true, forKey: "debugForceCanSendMessages")
        XCTAssertTrue(
            MessageComposerView.canCompose(phoneNumbers: ["555-0100"]),
            "a phone number plus a device that can send text should be composable"
        )
    }

    func testTracksCanSendMessagesExactly() {
        // Whatever `canSendMessages()` reports right now, `canCompose` with a
        // non-empty phone list must agree — no independently-derived check.
        XCTAssertEqual(
            MessageComposerView.canCompose(phoneNumbers: ["555-0100"]),
            MessageComposerView.canSendMessages()
        )
    }
}
