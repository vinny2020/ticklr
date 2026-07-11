import Foundation

/// Composes the user-facing toast message for a completed contact import
/// (TIC-85) — the iPhone Contacts path (`ContactImportService`) and the
/// LinkedIn CSV path (`LinkedInCSVParser`) share this so both toasts read
/// identically regardless of source. Never returns silence: a zero-imported
/// success (every contact was already a duplicate) still gets a "no new
/// contacts" style message rather than nothing.
enum ImportFeedback {
    static func message(imported: Int, skipped: Int) -> String {
        let headline = imported > 0
            ? String(localized: "import.toast.imported \(imported)")
            : String(localized: "import.toast.noneImported")

        guard skipped > 0 else { return headline }

        let skippedPhrase = String(localized: "import.toast.skippedSuffix \(skipped)")
        return "\(headline) · \(skippedPhrase)"
    }
}
