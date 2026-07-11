package com.xaymaca.sit.ui.nav

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaymaca.sit.ui.groups.GroupDetailScreen
import com.xaymaca.sit.ui.groups.GroupListScreen
import com.xaymaca.sit.ui.network.ContactDetailScreen
import com.xaymaca.sit.ui.network.NetworkListScreen
import com.xaymaca.sit.ui.shared.PendingPhoneChoice
import com.xaymaca.sit.ui.shared.PhoneChoice
import com.xaymaca.sit.ui.shared.PhoneChooser
import com.xaymaca.sit.ui.shared.PhoneNumberChooserDialog
import com.xaymaca.sit.ui.shared.WordmarkLockup
import com.xaymaca.sit.ui.tickle.TickleActionContent
import com.xaymaca.sit.ui.tickle.TickleEditScreen
import com.xaymaca.sit.ui.tickle.TickleListScreen
import com.xaymaca.sit.ui.tickle.TickleViewModel

// ---------------------------------------------------------------------------
// Two-pane list-detail scaffolds for the master-detail flows, shown only at
// expanded width (tablets / unfolded foldables / desktop windows). At compact
// and medium widths the NavGraph routes to the standalone list screens instead,
// which navigate to full-screen detail routes — preserving the phone UX exactly.
//
// Selection lives in the ListDetailPaneScaffold navigator (keyed by the row's
// Long id), not the nav back stack, so list and detail coexist side by side.
// The id type is nullable: the navigator's initial destination carries a null
// content, which renders the empty-detail placeholder. Cross-section jumps
// (group member -> contact, contact -> compose/edit/tickle) still go through
// the nav callbacks supplied by NavGraph.
// ---------------------------------------------------------------------------

/** Sentinel content key meaning "compose a new tickle" in the detail pane. */
private const val NEW_TICKLE_KEY = -1L

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NetworkPane(
    onAddContact: () -> Unit,
    onImport: () -> Unit,
    onEditContact: (Long) -> Unit,
    onAddTickleForContact: (Long) -> Unit,
    onCompose: (contactId: Long, reminderId: Long?) -> Unit,
    /**
     * TIC-96: a contact to land on directly in the detail slot, e.g. when a
     * cross-section jump (a group member tap in GroupsPane) targets Network
     * instead of the full-screen ContactDetail route — the jump stays inside
     * the pane world instead of leaving it. Null for the ordinary tab entry.
     */
    initialContactId: Long? = null,
) {
    // Seeds the navigator's destination history so a jump-in lands straight on
    // the requested contact's detail pane rather than the empty placeholder.
    val initialHistory = remember(initialContactId) {
        if (initialContactId != null) {
            listOf(
                ThreePaneScaffoldDestinationItem<Long?>(ListDetailPaneScaffoldRole.List),
                ThreePaneScaffoldDestinationItem<Long?>(ListDetailPaneScaffoldRole.Detail, initialContactId),
            )
        } else {
            listOf(ThreePaneScaffoldDestinationItem<Long?>(ListDetailPaneScaffoldRole.List))
        }
    }
    val navigator = rememberListDetailPaneScaffoldNavigator(initialDestinationHistory = initialHistory)

    BackHandler(enabled = navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                NetworkListScreen(
                    onContactClick = { id ->
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                    },
                    onAddContact = onAddContact,
                    onImport = onImport,
                    onCompose = onCompose,
                    onAddTickleForContact = onAddTickleForContact,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val contactId = navigator.currentDestination?.content
                if (contactId != null) {
                    ContactDetailScreen(
                        contactId = contactId,
                        onBack = { navigator.navigateBack() },
                        onAddTickle = { onAddTickleForContact(contactId) },
                        onEdit = { onEditContact(contactId) },
                        onCompose = onCompose,
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GroupsPane(
    onContactClick: (Long) -> Unit,
    onAddTickleForGroup: (Long) -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long?>()

    // TIC-88: id of a just-created group whose detail pane should auto-open the
    // Add Members sheet once. Cleared as soon as it's consumed so re-selecting
    // the same group later doesn't re-open the sheet.
    var autoOpenAddForId by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                GroupListScreen(
                    onGroupClick = { id ->
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                    },
                    onGroupCreated = { id ->
                        autoOpenAddForId = id
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val groupId = navigator.currentDestination?.content
                if (groupId != null) {
                    val openAdd = groupId == autoOpenAddForId
                    if (openAdd) autoOpenAddForId = null
                    GroupDetailScreen(
                        groupId = groupId,
                        openAddMembersOnLaunch = openAdd,
                        onBack = { navigator.navigateBack() },
                        onContactClick = onContactClick,
                        onAddTickle = { onAddTickleForGroup(groupId) },
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun TicklePane(
    onCompose: (contactId: Long, reminderId: Long?) -> Unit,
    onAddContact: () -> Unit,
) {
    // Shared with TickleListScreen and TickleEditScreen via the nav-entry-scoped
    // ViewModelStoreOwner, so a row tap's resolved `actionTarget` drives this pane.
    val viewModel: TickleViewModel = hiltViewModel()
    val actionTarget by viewModel.actionTarget.collectAsState()
    val context = LocalContext.current
    // TIC-96: a resolved multi-number choice awaiting the user's pick — see
    // PhoneChooser. Single-number targets keep the direct-dial fast path.
    var phoneChoice by remember { mutableStateOf<PendingPhoneChoice?>(null) }

    // Edit/new form state for the detail pane. NEW_TICKLE_KEY means "compose new".
    var editingTickleId by rememberSaveable { mutableStateOf<Long?>(null) }

    // A fresh row tap (new actionTarget) always wins the detail pane over any
    // lingering edit form, so tapping a row while editing switches to its actions.
    LaunchedEffect(actionTarget) {
        if (actionTarget != null) editingTickleId = null
    }

    // The navigator only supplies the scaffold directive/value (pane sizing + RTL
    // order). At expanded width both panes always show, so content is driven by
    // `editingTickleId` / `actionTarget` rather than navigator destinations.
    val navigator = rememberListDetailPaneScaffoldNavigator<Long?>()

    val detailVisible = editingTickleId != null || actionTarget != null
    BackHandler(enabled = detailVisible) {
        when {
            editingTickleId != null -> editingTickleId = null
            else -> viewModel.dismissActionSheet()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                TickleListScreen(
                    onAddTickle = {
                        viewModel.dismissActionSheet()
                        editingTickleId = NEW_TICKLE_KEY
                    },
                    onEditTickle = { id ->
                        viewModel.dismissActionSheet()
                        editingTickleId = id
                    },
                    onCompose = onCompose,
                    twoPane = true,
                    viewModel = viewModel,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val target = actionTarget
                when {
                    editingTickleId != null -> TickleEditScreen(
                        tickleId = editingTickleId?.takeIf { it != NEW_TICKLE_KEY },
                        preselectedContactId = null,
                        onSaved = { editingTickleId = null },
                        onBack = { editingTickleId = null },
                        onAddContact = onAddContact,
                        tickleViewModel = viewModel,
                    )
                    target != null -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 24.dp),
                        ) {
                            TickleActionContent(
                                target = target,
                                onCompose = {
                                    val contactId = target.reminder.contactId
                                    viewModel.dismissActionSheet()
                                    if (contactId != null) onCompose(contactId, target.reminder.id)
                                },
                                onCall = {
                                    when (val choice = PhoneChooser.choose(target.phones)) {
                                        is PhoneChoice.Direct ->
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${choice.number}")))
                                        is PhoneChoice.NeedsChoice ->
                                            phoneChoice = PendingPhoneChoice(choice.numbers) { number ->
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                                            }
                                        PhoneChoice.None -> {}
                                    }
                                    viewModel.dismissActionSheet()
                                },
                                onEmail = {
                                    target.emails.firstOrNull()?.let { email ->
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                    }
                                    viewModel.dismissActionSheet()
                                },
                                onMarkDone = {
                                    viewModel.markComplete(target.reminder)
                                    viewModel.dismissActionSheet()
                                },
                                onSnooze = { days ->
                                    viewModel.snooze(target.reminder, days)
                                    viewModel.dismissActionSheet()
                                },
                                onEdit = {
                                    val id = target.reminder.id
                                    viewModel.dismissActionSheet()
                                    editingTickleId = id
                                },
                            )
                        }
                    }
                    else -> EmptyDetailPlaceholder()
                }
            }
        },
    )

    phoneChoice?.let { pending ->
        PhoneNumberChooserDialog(
            numbers = pending.numbers,
            onSelect = { number -> pending.onChosen(number); phoneChoice = null },
            onDismiss = { phoneChoice = null },
        )
    }
}

/**
 * Detail pane shown when nothing is selected (only reachable at expanded width,
 * since narrower widths never render a standing-empty detail pane). Uses the
 * canonical Ticklr wordmark as a calm, on-brand empty state — no localized copy,
 * keeping brand strings untranslated and the translation-completeness test green.
 */
@Composable
private fun EmptyDetailPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            WordmarkLockup(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .alpha(0.85f),
            )
        }
    }
}
