package com.xaymaca.sit.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A pending "text now → remind me later" offer (TIC-86).
 *
 * When the user hands off a *plain* compose (one that carried no still-valid
 * mark-done reminder — see [PendingTickleCompletion]), there's no way to turn
 * "I just reached out" into a recurring nudge without re-navigating. Instead we
 * stash the recipient's id + display name here and, on return to the app, offer
 * "Create a tickle for [name]?" whose action opens TickleEdit prefilled with the
 * contact.
 *
 * This is the mutually-exclusive counterpart to [PendingTickleCompletion]:
 * [com.xaymaca.sit.ui.compose.ComposeViewModel.recordHandoff] stashes at most one
 * of the two per handoff — the mark-done prompt when a reminder applied; this
 * offer when none did AND the recipient has no live (non-COMPLETED) reminder;
 * nothing when they're already covered by one — so the user never sees both for
 * a single send, and is never invited to create a duplicate tickle.
 *
 * @param contactId   the [com.xaymaca.sit.data.model.Contact] to prefill TickleEdit with
 * @param contactName the recipient's display name, for the offer copy
 */
data class PendingTickleOffer(
    val contactId: Long,
    val contactName: String,
)

/**
 * App-scoped holder for a single [PendingTickleOffer] awaiting display.
 *
 * Same survival + collect semantics as [PendingTickleCompletionStore]: ComposeScreen
 * pops itself (`onDone()`) BEFORE launching the SMS intent, so the offer can't live
 * in screen state — this `@Singleton` is set from [com.xaymaca.sit.ui.compose.ComposeViewModel]
 * at handoff and observed by the app-level scaffold (`NavGraph`), which outlives the
 * pop and the round trip out to the SMS app. The observing effect must collect this
 * flow directly inside a `LaunchedEffect(Unit)` (NOT `collectAsState`): [consume]
 * emits `null`, and if that null were an effect-key change it would cancel the
 * running `showSnackbar` coroutine mid-display instead of landing as the next
 * sequential emission. The prompt's snackbar uses `SnackbarDuration.Indefinite` so
 * it survives the SMS round trip. Setting a new value replaces any un-shown one;
 * [consume] clears it after the offer is surfaced so it shows exactly once.
 */
@Singleton
class PendingTickleOfferStore @Inject constructor() {

    private val _pending = MutableStateFlow<PendingTickleOffer?>(null)
    val pending: StateFlow<PendingTickleOffer?> = _pending.asStateFlow()

    /** Stash an offer to surface on the user's next return to the app. */
    fun set(offer: PendingTickleOffer) {
        _pending.value = offer
    }

    /** Clear the stashed offer (after showing, or when it's no longer valid). */
    fun consume() {
        _pending.value = null
    }
}
