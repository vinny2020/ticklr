import Foundation
import SwiftData

struct LinkedInCSVParser {

    /// Parses a LinkedIn Connections CSV export and returns Contact models.
    /// Expected headers: First Name, Last Name, Email Address, Company, Position
    static func parse(url: URL, context: ModelContext) throws {
        let raw = try String(contentsOf: url, encoding: .utf8)
        let lines = raw.components(separatedBy: .newlines).filter { !$0.isEmpty }

        guard lines.count > 1 else { return }

        // LinkedIn CSVs have a few metadata lines before the actual header
        guard let headerIndex = lines.firstIndex(where: {
            $0.contains("First Name") && $0.contains("Last Name")
        }) else {
            throw CSVError.noHeaderFound
        }

        let headers = parseCSVLine(lines[headerIndex])
        let firstNameIdx = headers.firstIndex(of: "First Name") ?? 0
        let lastNameIdx  = headers.firstIndex(of: "Last Name") ?? 1
        let emailIdx     = headers.firstIndex(of: "Email Address")
        let companyIdx   = headers.firstIndex(of: "Company")
        let positionIdx  = headers.firstIndex(of: "Position")
        let phoneIdx     = headers.firstIndex(of: "Phone Number")

        var imported = 0
        for line in lines[(headerIndex + 1)...] {
            let fields = parseCSVLine(line)
            guard fields.count > max(firstNameIdx, lastNameIdx) else { continue }

            let contact = Contact(
                firstName:    fields[safe: firstNameIdx] ?? "",
                lastName:     fields[safe: lastNameIdx] ?? "",
                phoneNumbers: [fields[safe: phoneIdx] ?? ""].filter { !$0.isEmpty },
                emails:       [fields[safe: emailIdx] ?? ""].filter { !$0.isEmpty },
                company:      fields[safe: companyIdx] ?? "",
                jobTitle:     fields[safe: positionIdx] ?? "",
                importSource: .linkedin
            )
            context.insert(contact)
            imported += 1
        }

        try context.save()
        print("Ticklr: Imported \(imported) contacts from LinkedIn CSV")
    }

    private static func parseCSVLine(_ line: String) -> [String] {
        var fields: [String] = []
        var current = ""
        var inQuotes = false
        for char in line {
            if char == "\"" { inQuotes.toggle() }
            else if char == "," && !inQuotes {
                fields.append(current.trimmingCharacters(in: .whitespaces))
                current = ""
            } else { current.append(char) }
        }
        fields.append(current.trimmingCharacters(in: .whitespaces))
        return fields
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
