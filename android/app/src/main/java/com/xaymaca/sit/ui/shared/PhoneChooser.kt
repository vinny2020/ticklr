package com.xaymaca.sit.ui.shared

/**
 * TIC-96: the outcome of resolving a contact's phone numbers into an action
 * (dial / text). A single number takes the fast path automatically; more than
 * one means the caller can't silently guess which the user meant.
 */
sealed class PhoneChoice {
    data class Direct(val number: String) : PhoneChoice()
    data class NeedsChoice(val numbers: List<String>) : PhoneChoice()
    object None : PhoneChoice()
}

/**
 * TIC-96: shared decision for every Send/Call entry point that used to reach for
 * `phones.firstOrNull()` — ComposeScreen's Send button, ContactDetailScreen's
 * Call chip, TickleActionSheet's Call row (phone + tablet pane), and the new
 * Network row long-press quick actions. A contact with a home + mobile number
 * always silently dialed/texted whichever happened to be first in the stored
 * list; this makes the multi-number case an explicit choice instead.
 */
object PhoneChooser {
    fun choose(phones: List<String>): PhoneChoice = when {
        phones.isEmpty() -> PhoneChoice.None
        phones.size == 1 -> PhoneChoice.Direct(phones[0])
        else -> PhoneChoice.NeedsChoice(phones)
    }
}

/**
 * A [PhoneChoice.NeedsChoice] awaiting the user's pick, plus what to do once
 * they've made it. Held as composable-local state at each call site so the
 * chooser dialog can be shown modally and the original action (dial, text,
 * SMS handoff) resumed with the chosen number.
 */
data class PendingPhoneChoice(
    val numbers: List<String>,
    val onChosen: (String) -> Unit,
)
