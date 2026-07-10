package com.xaymaca.sit.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped holder for a single one-shot snackbar message awaiting display (TIC-84).
 *
 * `TickleEditScreen`'s save button used to block behind `delay(2000)` purely so
 * its own in-screen `TicklrToast` had time to be seen before the screen popped.
 * TIC-84 makes save-then-pop immediate instead, so the confirmation has to live
 * somewhere that survives the pop — this `@Singleton` mirrors
 * [PendingTickleCompletionStore]'s pattern: it's set from a ViewModel's
 * `viewModelScope` (which isn't cancelled when the screen's composition is torn
 * down by the pop) and observed by the app-level scaffold (`NavGraph`), which
 * outlives any individual screen. Setting a new message replaces any un-shown
 * one; [consume] clears it after the snackbar is surfaced so it shows exactly
 * once.
 *
 * Same consume-then-collect mechanics as [PendingTickleCompletionStore] apply
 * to whoever observes [pending]: collect the flow directly inside a
 * `LaunchedEffect(Unit)` (NOT `collectAsState`) — `consume()` emits `null`,
 * and if that null were an effect key change it would cancel the running
 * `showSnackbar` coroutine mid-display instead of landing as the next
 * sequential emission after `showSnackbar` returns.
 */
@Singleton
class PendingSnackbarMessageStore @Inject constructor() {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Stash a message to surface on the app-level snackbar host. */
    fun set(message: String) {
        _pending.value = message
    }

    /** Clear the stashed message (after showing, or when it's no longer valid). */
    fun consume() {
        _pending.value = null
    }
}
