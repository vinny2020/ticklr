package com.xaymaca.sit.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A pending "did that send land? — mark the tickle done?" prompt (TIC-82).
 *
 * Android's `Intent.ACTION_SENDTO` SMS handoff gives no send-confirmation
 * signal, so we can't auto-complete a reminder on send. Instead, when the user
 * hands off a compose that carried a reminder id, we stash the reminder id +
 * the contact's display name here and prompt the user to mark the tickle done
 * when they return to the app.
 *
 * @param reminderId  the [com.xaymaca.sit.data.model.TickleReminder] id to complete
 * @param contactName the recipient's display name, for the prompt copy
 */
data class PendingTickleCompletion(
    val reminderId: Long,
    val contactName: String,
)

/**
 * App-scoped holder for a single [PendingTickleCompletion] awaiting display.
 *
 * ComposeScreen pops itself (`onDone()`) BEFORE launching the SMS intent, so the
 * prompt has to survive that pop — it can't live in the compose screen's state.
 * This `@Singleton` is set from [com.xaymaca.sit.ui.compose.ComposeViewModel]
 * at handoff and observed by the app-level scaffold (`NavGraph`), which is alive
 * across the pop and the trip out to the SMS app. Setting a new pending value
 * replaces any un-shown one; [consume] clears it after the prompt is surfaced so
 * it shows exactly once.
 */
@Singleton
class PendingTickleCompletionStore @Inject constructor() {

    private val _pending = MutableStateFlow<PendingTickleCompletion?>(null)
    val pending: StateFlow<PendingTickleCompletion?> = _pending.asStateFlow()

    /** Stash a prompt to surface on the user's next return to the app. */
    fun set(completion: PendingTickleCompletion) {
        _pending.value = completion
    }

    /** Clear the stashed prompt (after showing, or when it's no longer valid). */
    fun consume() {
        _pending.value = null
    }
}
