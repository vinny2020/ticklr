package com.xaymaca.sit.ui.compose

/**
 * TIC-90: pure decision for whether applying a message template should prompt
 * to replace the current draft, rather than overwrite it silently. Extracted
 * from [ComposeViewModel] so the rule is unit-testable without a
 * ViewModel/Hilt.
 *
 * Applying overwrites silently when the draft is empty, or when it exactly
 * matches the body of the template most recently applied — in both cases
 * nothing the user typed is at risk of being lost. It prompts for anything
 * else: hand-typed text that was never a template, or a template body the
 * user has since edited.
 */
object TemplateApplyDecision {
    fun shouldConfirmReplace(currentBody: String, lastAppliedTemplateBody: String?): Boolean {
        if (currentBody.isBlank()) return false
        return currentBody != lastAppliedTemplateBody
    }
}
