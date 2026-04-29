import Foundation
import SwiftData

enum MessageTemplateSeed {
    static let defaultTitle = "Checking in"
    static let defaultBody  = "Hey, just wanted to check in! Hope you're doing well 😊"

    private static let seededFlagKey = "hasSeededDefaultTemplates"

    static func seedIfNeeded(container: ModelContainer) {
        guard !UserDefaults.standard.bool(forKey: seededFlagKey) else { return }
        let context = ModelContext(container)
        context.insert(MessageTemplate(title: defaultTitle, body: defaultBody))
        try? context.save()
        UserDefaults.standard.set(true, forKey: seededFlagKey)
    }
}
