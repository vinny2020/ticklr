import Foundation
import SwiftData

struct LinkedInCSVParser {

    /// Parses a LinkedIn Connections CSV export and returns Contact models.
    /// Expected headers: First Name, Last Name, Email Address, Company, Position
    ///
    /// The whole file is tokenized as a character stream (RFC 4180) rather than
    /// line-by-line, so a quoted field may contain embedded commas AND newlines
    /// without splitting one connection across two records. A leading UTF-8 BOM
    /// is stripped. Kept in lockstep with the Android LinkedInCSVParser.
    @discardableResult
    static func parse(url: URL, context: ModelContext) throws -> (imported: Int, skipped: Int) {
        let raw = try String(contentsOf: url, encoding: .utf8)
        let records = parseCSV(raw)

        guard let headerIndex = records.firstIndex(where: isHeaderRow) else {
            throw CSVError.noHeaderFound
        }

        let headers = records[headerIndex]
        let firstNameIdx = headers.firstIndex(of: "First Name") ?? 0
        let lastNameIdx  = headers.firstIndex(of: "Last Name") ?? 1
        let emailIdx     = headers.firstIndex(of: "Email Address")
        let companyIdx   = headers.firstIndex(of: "Company")
        let positionIdx  = headers.firstIndex(of: "Position")
        let phoneIdx     = headers.firstIndex(of: "Phone Number")

        var imported = 0
        var skipped = 0

        // Fetch existing fingerprints to detect duplicates. Mutated as we go so
        // duplicates *within* this same file are also caught, not just ones that
        // already existed in the store before the loop started.
        var seenFingerprints: Set<String> = {
            let descriptor = FetchDescriptor<Contact>()
            let all = (try? context.fetch(descriptor)) ?? []
            return Set(all.map(\.fingerprint).filter { !$0.isEmpty })
        }()

        for fields in records[(headerIndex + 1)...] {
            guard fields.contains(where: { !$0.isEmpty }) else { continue }
            guard fields.count > max(firstNameIdx, lastNameIdx) else { continue }

            let firstName    = fields[safe: firstNameIdx] ?? ""
            let lastName     = fields[safe: lastNameIdx] ?? ""
            if firstName.isEmpty && lastName.isEmpty { continue }

            let phoneNumbers = [fields[safe: phoneIdx] ?? ""].filter { !$0.isEmpty }
            let emails       = [fields[safe: emailIdx] ?? ""].filter { !$0.isEmpty }

            let fingerprint = ContactFingerprint.compute(
                firstName: firstName,
                lastName: lastName,
                phoneNumbers: phoneNumbers,
                emails: emails
            )

            // Skip if already exists (in the store or earlier in this file)
            if !fingerprint.isEmpty && seenFingerprints.contains(fingerprint) {
                skipped += 1
                continue
            }

            let contact = Contact(
                firstName:    firstName,
                lastName:     lastName,
                phoneNumbers: phoneNumbers,
                emails:       emails,
                company:      fields[safe: companyIdx] ?? "",
                jobTitle:     fields[safe: positionIdx] ?? "",
                importSource: .linkedin,
                fingerprint:  fingerprint
            )
            context.insert(contact)
            if !fingerprint.isEmpty { seenFingerprints.insert(fingerprint) }
            imported += 1
        }

        try context.save()
        print("Ticklr: Imported \(imported) contacts, skipped \(skipped) duplicates")
        return (imported: imported, skipped: skipped)
    }

    /// LinkedIn CSVs carry preamble/notes lines before the column row.
    private static func isHeaderRow(_ fields: [String]) -> Bool {
        fields.contains("First Name") && fields.contains("Last Name")
    }

    /// Tokenizes an entire CSV document into records of fields (RFC 4180).
    /// Quoted fields may contain commas, CR/LF, and escaped quotes ("").
    /// A leading UTF-8 BOM is stripped. Fields are whitespace-trimmed.
    private static func parseCSV(_ input: String) -> [[String]] {
        var text = Substring(input)
        if text.first == "\u{FEFF}" { text = text.dropFirst() }

        var records: [[String]] = []
        var record: [String] = []
        var current = ""
        var inQuotes = false

        func endField() {
            record.append(current.trimmingCharacters(in: .whitespacesAndNewlines))
            current = ""
        }
        func endRecord() {
            endField()
            records.append(record)
            record = []
        }

        var iterator = text.makeIterator()
        var pending: Character? = iterator.next()

        while let char = pending {
            pending = iterator.next()

            if inQuotes {
                if char == "\"" {
                    if pending == "\"" {           // escaped quote ("") → literal quote
                        current.append("\"")
                        pending = iterator.next()
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(char)
                }
            } else {
                switch char {
                case "\"":
                    inQuotes = true
                case ",":
                    endField()
                case "\r":
                    endRecord()
                    if pending == "\n" { pending = iterator.next() }  // consume CRLF
                case "\n":
                    endRecord()
                default:
                    current.append(char)
                }
            }
        }
        // Flush the final field/record unless the file ended on a newline.
        if !current.isEmpty || !record.isEmpty {
            endRecord()
        }
        return records
    }

    enum CSVError: Error {
        case noHeaderFound
    }
}

private extension Array {
    subscript(safe index: Int?) -> Element? {
        guard let i = index, i >= 0, i < count else { return nil }
        return self[i]
    }
}
