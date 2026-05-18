import Foundation
import UIKit

/// On-device store for user-attached contact photos. The system Contacts
/// entry is never written to (Ticklr is read-only on the address book per
/// HANDOFF Privacy promises). Photos live under
///   Library/Application Support/Ticklr/ContactPhotos/{uuid}.jpg
/// and are excluded from iCloud backup.
enum PhotoStore {
    private static let folderName = "ContactPhotos"
    private static let maxEdge: CGFloat = 512
    private static let jpegQuality: CGFloat = 0.85

    static func localImage(for contactId: UUID) -> UIImage? {
        let url = url(for: contactId)
        guard FileManager.default.fileExists(atPath: url.path),
              let data = try? Data(contentsOf: url),
              let image = UIImage(data: data) else { return nil }
        return image
    }

    static func save(_ image: UIImage, for contactId: UUID) throws {
        let resized = resize(image, maxEdge: maxEdge)
        guard let data = resized.jpegData(compressionQuality: jpegQuality) else {
            throw NSError(domain: "PhotoStore", code: 1,
                          userInfo: [NSLocalizedDescriptionKey: "JPEG encode failed"])
        }
        let url = try ensureURL(for: contactId)
        try data.write(to: url, options: .atomic)
        try excludeFromBackup(url)
    }

    static func delete(for contactId: UUID) {
        try? FileManager.default.removeItem(at: url(for: contactId))
    }

    /// Wipes every user-attached photo. Used by Settings → Clear All Data;
    /// single-contact deletion already calls `delete(for:)`.
    static func deleteAll() {
        try? FileManager.default.removeItem(at: folderURL)
    }

    // MARK: - Internals

    private static var folderURL: URL {
        let support = FileManager.default.urls(for: .applicationSupportDirectory,
                                               in: .userDomainMask)[0]
        return support.appendingPathComponent("Ticklr/\(folderName)", isDirectory: true)
    }

    private static func url(for contactId: UUID) -> URL {
        folderURL.appendingPathComponent("\(contactId.uuidString).jpg")
    }

    private static func ensureURL(for contactId: UUID) throws -> URL {
        if !FileManager.default.fileExists(atPath: folderURL.path) {
            try FileManager.default.createDirectory(at: folderURL,
                                                    withIntermediateDirectories: true)
        }
        return url(for: contactId)
    }

    private static func excludeFromBackup(_ url: URL) throws {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var u = url
        try u.setResourceValues(values)
    }

    private static func resize(_ image: UIImage, maxEdge: CGFloat) -> UIImage {
        let w = image.size.width
        let h = image.size.height
        let longest = max(w, h)
        guard longest > maxEdge else { return cropToSquare(image) }
        let scale = maxEdge / longest
        let newSize = CGSize(width: w * scale, height: h * scale)
        let renderer = UIGraphicsImageRenderer(size: newSize)
        let scaled = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
        return cropToSquare(scaled)
    }

    private static func cropToSquare(_ image: UIImage) -> UIImage {
        let edge = min(image.size.width, image.size.height)
        let origin = CGPoint(x: (image.size.width - edge) / 2,
                             y: (image.size.height - edge) / 2)
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: edge, height: edge))
        return renderer.image { _ in
            image.draw(at: CGPoint(x: -origin.x, y: -origin.y))
        }
    }
}
