package com.xaymaca.sit

import com.xaymaca.sit.ui.compose.TemplateApplyDecision
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TIC-90: applying a template over a typed draft should prompt to replace it
 * rather than clobbering it silently — except when there's nothing at risk
 * (an empty body) or the body is exactly what the last-applied template set it
 * to (nothing typed since, so a second application isn't lossy).
 */
class TemplateApplyDecisionTest {

    @Test
    fun `empty body never prompts`() {
        assertFalse(TemplateApplyDecision.shouldConfirmReplace("", null))
        assertFalse(TemplateApplyDecision.shouldConfirmReplace("", "Hey! Just checking in."))
    }

    @Test
    fun `blank (whitespace-only) body never prompts`() {
        assertFalse(TemplateApplyDecision.shouldConfirmReplace("   ", "Hey! Just checking in."))
    }

    @Test
    fun `body unchanged since the last template applied does not prompt`() {
        assertFalse(
            TemplateApplyDecision.shouldConfirmReplace(
                currentBody = "Hey! Just checking in.",
                lastAppliedTemplateBody = "Hey! Just checking in.",
            )
        )
    }

    @Test
    fun `hand-typed text that was never a template prompts`() {
        assertTrue(
            TemplateApplyDecision.shouldConfirmReplace(
                currentBody = "Hey, wanted to reach out about the thing we discussed.",
                lastAppliedTemplateBody = null,
            )
        )
    }

    @Test
    fun `a template body edited since it was applied prompts`() {
        assertTrue(
            TemplateApplyDecision.shouldConfirmReplace(
                currentBody = "Hey! Just checking in — and also about Friday.",
                lastAppliedTemplateBody = "Hey! Just checking in.",
            )
        )
    }
}
