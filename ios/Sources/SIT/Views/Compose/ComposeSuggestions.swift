import Foundation
import MessageUI

/// A lightweight, `Sendable` handle to a contact the app offers to create a
/// tickle for after a plain (reminder-less) send (TIC-86). Carries the stable
/// `id` and a display name only — never a live `@Model` reference — so it can
/// survive the compose surface dismissing and be handed to whatever screen
/// hosts the offer toast, which re-fetches the `Contact` by `id` when tapped.
struct TickleSuggestion: Identifiable, Equatable, Sendable {
    let contactID: UUID
    let contactName: String
    var id: UUID { contactID }
}

/// Which post-send surface a Compose send should present. Exactly one, or none.
/// Encodes the TIC-82 ↔ TIC-86 exclusivity rule: a send that had a due reminder
/// attached routes to the completion/undo toast (`.completionUndo`); a send with
/// no reminder attached routes to the create-a-tickle offer (`.suggestTickle`) —
/// but only when the contact has no other open (active/snoozed) reminder, so
/// someone already covered by an upcoming tickle isn't offered a duplicate.
/// The two actionable surfaces are mutually exclusive — never both for one send.
enum PostSendSurface: Equatable {
    /// Present nothing: not a confirmed send (cancelled / failed / nil), or a
    /// plain send to a contact already covered by an active/snoozed reminder.
    case none
    /// Confirmed send that had a due reminder attached — the TIC-82 auto-complete
    /// + undo toast owns this case. (Whether a snapshot actually materializes is
    /// a separate live-store re-validation; either way the offer never shows.)
    case completionUndo
    /// Confirmed send with no reminder attached and no other open reminder on
    /// the contact — offer to create a tickle.
    case suggestTickle
}

/// Pure assembly of Compose's recipient suggestions and its post-send routing.
/// Extracted from the view so both are unit-testable without SwiftUI.
enum ComposeSuggestions {

    /// Default cap on the Recents section — a sensible handful, not the whole book.
    static let recentsCap = 5

    /// Builds the two recipient-suggestion sections shown when the To-field is
    /// empty (TIC-86):
    ///  - `dueToday`: every contact with at least one currently-due reminder
    ///    (same `isDueForSendCompletion` semantics as send-completion), ordered
    ///    **most overdue first** (earliest due date first).
    ///  - `recents`: contacts with a non-nil `lastContactedAt`, most-recent first,
    ///    excluding anyone already in `dueToday`, capped at `cap`.
    ///
    /// Pure over the passed-in array — no `@Query`, no context — so it can be
    /// exercised directly in tests.
    static func assemble(
        from contacts: [Contact],
        now: Date = Date(),
        cap: Int = recentsCap
    ) -> (dueToday: [Contact], recents: [Contact]) {
        // Due today: keep each contact's *earliest* due reminder date so the
        // most-overdue contact sorts first.
        var duePairs: [(contact: Contact, earliestDue: Date)] = []
        for contact in contacts {
            let earliest = contact.tickles
                .filter { TickleScheduler.isDueForSendCompletion($0, now: now) }
                .map(\.nextDueDate)
                .min()
            if let earliest {
                duePairs.append((contact, earliest))
            }
        }
        let dueToday = duePairs
            .sorted { $0.earliestDue < $1.earliestDue }
            .map(\.contact)

        let dueIDs = Set(dueToday.map(\.id))
        let recents = contacts
            .filter { $0.lastContactedAt != nil && !dueIDs.contains($0.id) }
            .sorted { ($0.lastContactedAt ?? .distantPast) > ($1.lastContactedAt ?? .distantPast) }
            .prefix(cap)

        return (dueToday, Array(recents))
    }

    /// Whether `contact` already has any open (non-completed) reminder — active
    /// or snoozed, due or not. Same status-filter pattern as ContactDetailView's
    /// `dueReminder` lookup, minus the due-date check: for blocking the offer,
    /// *any* upcoming coverage counts, not just currently-due.
    static func hasOpenReminder(_ contact: Contact) -> Bool {
        contact.tickles.contains { $0.status == .active || $0.status == .snoozed }
    }

    /// The exclusivity decision: given a composer result, whether a due reminder
    /// was attached to this compose, and whether the texted contact already has
    /// any open (active/snoozed) reminder, which surface (if any) the send should
    /// present. Three outcomes, at most one per send:
    ///  - reminder attached → `.completionUndo` (TIC-82, unchanged) — keyed on
    ///    *whether a reminder was attached*, not on whether completion succeeded,
    ///    so a reminder-attached send never also gets the offer;
    ///  - no reminder attached AND the contact has no open reminder →
    ///    `.suggestTickle`;
    ///  - otherwise → `.none` — a contact already covered by an active/snoozed
    ///    tickle (just not currently due) must not be offered a duplicate.
    static func postSendSurface(
        result: MessageComposeResult?,
        hadDueReminderAttached: Bool,
        contactHasOpenReminder: Bool
    ) -> PostSendSurface {
        guard result == .sent else { return .none }
        if hadDueReminderAttached { return .completionUndo }
        return contactHasOpenReminder ? .none : .suggestTickle
    }
}
