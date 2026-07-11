package com.xaymaca.sit.ui.nav

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.xaymaca.sit.R
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.ui.compose.ComposeScreen
import com.xaymaca.sit.ui.groups.GroupDetailScreen
import com.xaymaca.sit.ui.groups.GroupListScreen
import com.xaymaca.sit.ui.network.AddContactScreen
import com.xaymaca.sit.ui.network.ContactDetailScreen
import com.xaymaca.sit.ui.network.NetworkListScreen
import com.xaymaca.sit.ui.onboarding.ImportScreen
import com.xaymaca.sit.ui.onboarding.OnboardingScreen
import com.xaymaca.sit.ui.settings.SettingsScreen
import com.xaymaca.sit.ui.settings.TemplateEditScreen
import com.xaymaca.sit.ui.settings.TemplateListScreen
import com.xaymaca.sit.ui.tickle.TickleEditScreen
import com.xaymaca.sit.ui.tickle.TickleListScreen
import com.xaymaca.sit.ui.tickle.TickleViewModel

/**
 * The graph's start destination, chosen once from persisted onboarding state.
 * Kept pure (no Compose/Android deps) so it can be unit-tested and so callers
 * read it a single time at NavHost creation — reading it reactively mid-session
 * rebuilds the graph and wipes the back stack (TIC-64).
 */
internal fun startDestinationFor(onboardingComplete: Boolean): String =
    if (onboardingComplete) Screen.Tickle.route else Screen.Onboarding.route

/**
 * TIC-94: which flow launched Import — onboarding (first-run) vs in-app
 * (Settings row / Network overflow menu, reached mid-session). Parsed from
 * the `origin` nav arg; anything other than the exact onboarding sentinel —
 * including a missing/null arg — defaults to [ImportOrigin.InApp], the
 * non-destructive choice, rather than onboarding's stack-wiping one.
 */
internal enum class ImportOrigin { Onboarding, InApp }

internal fun importOriginFor(origin: String?): ImportOrigin =
    if (origin == Screen.Import.ORIGIN_ONBOARDING) ImportOrigin.Onboarding else ImportOrigin.InApp

/**
 * TIC-94: what an Import completion (success or skip) should do to the back
 * stack. Onboarding has no meaningful stack yet, so it resets to a fresh tab
 * ([FreshStart] — the caller picks Network for success, Tickle for skip). An
 * in-app entry (Settings, Network overflow) has a real stack underneath it
 * that must survive, so it just returns to whatever launched Import
 * ([ReturnToCaller]) instead of the old blanket `popUpTo(0)`.
 */
internal enum class ImportExit { FreshStart, ReturnToCaller }

internal fun importExitFor(origin: ImportOrigin): ImportExit =
    if (origin == ImportOrigin.Onboarding) ImportExit.FreshStart else ImportExit.ReturnToCaller

private data class BottomNavItem(
    val screen: Screen,
    @StringRes val labelResId: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Network, R.string.nav_network, Icons.Default.People),
    BottomNavItem(Screen.GroupList, R.string.nav_groups, Icons.Default.Group),
    BottomNavItem(Screen.Tickle, R.string.nav_tickle, Icons.Default.Notifications),
    BottomNavItem(Screen.Compose, R.string.nav_compose, Icons.Default.Email),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings)
)

private val bottomNavRoutes = setOf(
    Screen.Network.route,
    Screen.Tickle.route,
    Screen.GroupList.route,
    Screen.Compose.route,
    Screen.Settings.route
)

@Composable
fun NavGraph(widthSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)

    val navController = rememberNavController()

    // Compute the graph's start destination ONCE, at NavHost creation. Reading the
    // onboarding pref reactively inside composition is a trap (TIC-64): completing
    // onboarding writes the pref and navigates, the navigate triggers a
    // recomposition (currentBackStackEntryAsState), the pref re-read flips this
    // value, and NavHost rebuilds its graph with a new startDestinationId — which
    // pops the entire back stack and discards the screen we just pushed. Keeping it
    // stable lets the explicit navigate(...) { popUpTo } own onboarding transitions.
    // Process death/recreation re-reads the persisted pref here, so a completed
    // onboarding never resurrects.
    val startDestination = remember {
        startDestinationFor(prefs.getBoolean(SITApp.KEY_ONBOARDING_COMPLETE, false))
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Primary navigation chrome (rail or bottom bar) shows only on the five
    // top-level destinations, not on detail/edit screens.
    val showNavChrome = currentDestination?.route
        ?.substringBefore('?')
        ?.substringBefore('/') in bottomNavRoutes

    // Medium/expanded widths (tablets, unfolded foldables, ChromeOS/desktop windows)
    // get a side NavigationRail; compact (phones) keeps the bottom NavigationBar.
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact

    // Only the widest layouts get side-by-side list-detail. At compact/medium the
    // master-detail flows keep navigating to full-screen detail routes, preserving
    // the immersive (chrome-free) phone detail experience.
    val useTwoPane = widthSizeClass == WindowWidthSizeClass.Expanded

    // TIC-82: the "mark [name]'s tickle done?" prompt. The pending completion is
    // stashed in an app-scoped singleton at SMS handoff (ComposeViewModel) and
    // observed here — a level that outlives ComposeScreen's pre-handoff pop and
    // the round trip out to the SMS app — via an activity-scoped TickleViewModel
    // (LocalViewModelStoreOwner is the Activity here, so this is a stable
    // instance across recompositions). Marking done routes through the existing
    // TickleViewModel.markComplete / TickleScheduler path so alarms stay in sync.
    //
    // Mechanics: the effect is keyed on Unit and collects the StateFlow itself,
    // NOT via collectAsState — consume() emits null, and if that null were an
    // effect key change it would cancel the running coroutine mid-showSnackbar
    // and dismiss the prompt after one frame. collect processes emissions
    // sequentially, so the null lands after showSnackbar returns and no-ops.
    // Duration is Indefinite: the handoff backgrounds us before the user reads
    // anything, and a timed snackbar's delay() keeps counting while we're
    // covered by the SMS app — a Long (~10s) prompt would be gone before the
    // user got back. withDismissAction gives an explicit dismiss instead.
    val snackbarHostState = remember { SnackbarHostState() }
    val tickleViewModel: TickleViewModel = hiltViewModel()
    val markDoneLabel = stringResource(R.string.tickle_prompt_mark_done)
    LaunchedEffect(Unit) {
        tickleViewModel.pendingTickleCompletion.collect { pending ->
            if (pending == null) return@collect
            // Consume first so the prompt shows exactly once.
            tickleViewModel.consumePendingTickleCompletion()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.tickle_prompt_message, pending.contactName),
                actionLabel = markDoneLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                tickleViewModel.completePendingTickle(pending.reminderId)
            }
        }
    }

    // TIC-84: TickleEditScreen's save now applies and pops immediately (no
    // artificial delay), so its save-confirmation ("Tickle saved"/"Tickle
    // updated") can no longer live as a screen-local toast — it wouldn't
    // survive the pop. TickleViewModel.upsert() posts it to the same
    // app-scoped PendingSnackbarMessageStore pattern as the TIC-82 prompt
    // above, and this effect surfaces it on the shared snackbarHostState.
    // Short duration is fine here (unlike the TIC-82 prompt, the app stays
    // foregrounded through save+pop, so there's no risk of the timed
    // dismissal ticking away while we're covered by another app).
    LaunchedEffect(Unit) {
        tickleViewModel.pendingSnackbarMessage.collect { message ->
            if (message == null) return@collect
            // Consume first so the message shows exactly once.
            tickleViewModel.consumePendingSnackbarMessage()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    // TIC-86: the "create a tickle for [name]?" offer — stashed at a plain SMS
    // handoff (ComposeViewModel, when no mark-done reminder applied) and surfaced
    // here, the level that outlives ComposeScreen's pre-handoff pop and the trip
    // out to the SMS app. Same hard-won mechanics as the TIC-82 prompt above:
    // collect the StateFlow directly inside LaunchedEffect(Unit) (NOT
    // collectAsState) so consume()'s null lands as the next sequential emission
    // rather than cancelling the running showSnackbar; and Indefinite duration so
    // a timed delay() isn't ticking away while we're covered by the SMS app.
    // Unlike the mark-done prompt (which mutates a reminder via TickleViewModel),
    // this action just navigates — TickleEdit prefilled with the contact — so it
    // needs the navController, available here at the collector site. The offer and
    // the mark-done prompt are mutually exclusive per handoff (ComposeViewModel
    // stashes at most one), so these two effects never both fire for one send.
    val createTickleLabel = stringResource(R.string.compose_tickle_offer_action)
    LaunchedEffect(Unit) {
        tickleViewModel.pendingTickleOffer.collect { offer ->
            if (offer == null) return@collect
            // Consume first so the offer shows exactly once.
            tickleViewModel.consumePendingTickleOffer()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.compose_tickle_offer_message, offer.contactName),
                actionLabel = createTickleLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                navController.navigate(Screen.TickleEdit.createRouteWithContact(offer.contactId))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showNavChrome && !useRail) {
                AppBottomNavBar(currentDestination, navController)
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showNavChrome && useRail) {
                AppNavigationRail(currentDestination, navController)
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onImportContacts = {
                        navController.navigate(Screen.Import.createRoute(Screen.Import.ORIGIN_ONBOARDING))
                    },
                    onAddContact = {
                        // Warm-redesign: second CTA replaces "Start Empty"
                        // with "Add my first contact" — mark onboarding complete,
                        // then open the AddContact form with the Network home seeded
                        // beneath it so saving (or backing out of) the form returns
                        // to the app rather than an empty stack. Onboarding is popped
                        // inclusive so back never returns to it. startDestination is
                        // computed once (see above), so these navigations no longer
                        // rebuild the graph and pop the stack (TIC-64).
                        // TIC-85: lands on Network (not Tickle) so the user sees the
                        // contact they just added, instead of an empty tickle list.
                        prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).apply()
                        navController.navigate(Screen.Network.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                        navController.navigate(Screen.AddContact.route)
                    }
                )
            }

            // Import
            composable(
                route = Screen.Import.ROUTE,
                arguments = listOf(
                    navArgument(Screen.Import.ARG_ORIGIN) {
                        type = NavType.StringType
                        defaultValue = Screen.Import.ORIGIN_IN_APP
                    }
                )
            ) { backStackEntry ->
                // TIC-94: a successful import auto-advances (TIC-85, no "Continue"
                // tap) and "Skip for now" bails out — but where either lands
                // depends on how Import was reached. Onboarding has no back
                // stack worth keeping, so both reset to a fresh tab (Network for
                // success so the user sees what just landed; Tickle for skip,
                // whose empty state carries the onboarding hero CTA). An in-app
                // entry (Settings row, Network overflow menu) has a real stack
                // underneath — popUpTo(0) there used to teleport the user to
                // Network and destroy it. Both now just popBackStack() to
                // return to wherever the user was. The TIC-85 count snackbar
                // still shows post-pop since it rides the app-level
                // PendingSnackbarMessageStore/TickleViewModel-observed state
                // above, not screen-local state.
                val origin = importOriginFor(
                    backStackEntry.arguments?.getString(Screen.Import.ARG_ORIGIN)
                )
                ImportScreen(
                    onImportSuccess = {
                        when (importExitFor(origin)) {
                            ImportExit.FreshStart -> {
                                prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).apply()
                                navController.navigate(Screen.Network.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            ImportExit.ReturnToCaller -> navController.popBackStack()
                        }
                    },
                    onSkip = {
                        when (importExitFor(origin)) {
                            ImportExit.FreshStart -> {
                                prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).apply()
                                navController.navigate(Screen.Tickle.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            // TIC-94: in-app "Skip for now" now behaves exactly
                            // like the back arrow (plain popBackStack, no
                            // teleport) rather than hiding/relabeling the
                            // button — "Skip for now" still reads correctly as
                            // "leave without importing" outside onboarding, and
                            // reusing it avoids a new string across 21 locales.
                            ImportExit.ReturnToCaller -> navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Network
            composable(
                route = Screen.Network.ROUTE,
                arguments = listOf(
                    navArgument(Screen.Network.ARG_FOCUS_CONTACT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                // TIC-96: carried by cross-section jumps (GroupsPane's member tap)
                // that want to land on a specific contact's detail pane instead of
                // the plain list — absent for the ordinary tab entry.
                val focusContactId = backStackEntry.arguments
                    ?.getLong(Screen.Network.ARG_FOCUS_CONTACT_ID)?.takeIf { it != -1L }
                if (useTwoPane) {
                    NetworkPane(
                        onAddContact = { navController.navigate(Screen.AddContact.route) },
                        onImport = {
                            navController.navigate(Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP))
                        },
                        onEditContact = { id -> navController.navigate("edit_contact/$id") },
                        onAddTickleForContact = { id ->
                            navController.navigate(Screen.TickleEdit.createRouteWithContact(id))
                        },
                        onCompose = { id, reminderId ->
                            // Keep the current screen on the back stack so
                            // finishing (or backing out of) Compose returns here.
                            navController.navigate(Screen.Compose.createRoute(id, reminderId)) {
                                launchSingleTop = true
                            }
                        },
                        initialContactId = focusContactId,
                    )
                } else {
                    NetworkListScreen(
                        onContactClick = { id ->
                            navController.navigate(Screen.ContactDetail.createRoute(id))
                        },
                        onAddContact = { navController.navigate(Screen.AddContact.route) },
                        onImport = {
                            navController.navigate(Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP))
                        },
                        onCompose = { id, reminderId ->
                            navController.navigate(Screen.Compose.createRoute(id, reminderId)) {
                                launchSingleTop = true
                            }
                        },
                        onAddTickleForContact = { id ->
                            navController.navigate(Screen.TickleEdit.createRouteWithContact(id))
                        },
                    )
                }
            }

            composable(Screen.AddContact.route) {
                AddContactScreen(
                    contactId = null,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ContactDetail.ROUTE,
                arguments = listOf(navArgument("contactId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
                ContactDetailScreen(
                    contactId = contactId,
                    onBack = { navController.popBackStack() },
                    onAddTickle = { navController.navigate(Screen.TickleEdit.createRouteWithContact(contactId)) },
                    onEdit = { navController.navigate("edit_contact/$contactId") },
                    onCompose = { id, reminderId ->
                        navController.navigate(Screen.Compose.createRoute(id, reminderId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Edit contact reuses AddContactScreen with a contactId
            composable(
                route = "edit_contact/{contactId}",
                arguments = listOf(navArgument("contactId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong("contactId")
                AddContactScreen(
                    contactId = contactId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // Groups
            composable(Screen.GroupList.route) {
                if (useTwoPane) {
                    GroupsPane(
                        // TIC-96: at expanded width this stays inside the pane
                        // world — Network's detail pane, not the full-screen
                        // ContactDetail route — instead of abandoning the panes
                        // for a member tap.
                        onContactClick = { id ->
                            navController.navigate(Screen.Network.createRouteFocusingContact(id))
                        },
                        onAddTickleForGroup = { id ->
                            navController.navigate(Screen.TickleEdit.createRouteWithGroup(id))
                        },
                    )
                } else {
                    GroupListScreen(
                        onGroupClick = { id -> navController.navigate(Screen.GroupDetail.createRoute(id)) },
                        // TIC-88 create-with-members: jump straight into the new
                        // group with the Add Members sheet already open.
                        onGroupCreated = { id ->
                            navController.navigate(Screen.GroupDetail.createRouteWithAddMembers(id))
                        }
                    )
                }
            }

            composable(
                route = Screen.GroupDetail.ROUTE,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.LongType },
                    navArgument(Screen.GroupDetail.ARG_OPEN_ADD) {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                val openAdd = backStackEntry.arguments?.getBoolean(Screen.GroupDetail.ARG_OPEN_ADD) ?: false
                GroupDetailScreen(
                    groupId = groupId,
                    openAddMembersOnLaunch = openAdd,
                    onBack = { navController.popBackStack() },
                    onContactClick = { id ->
                        navController.navigate(Screen.ContactDetail.createRoute(id))
                    },
                    // TIC-88 group tickle creation path.
                    onAddTickle = {
                        navController.navigate(Screen.TickleEdit.createRouteWithGroup(groupId))
                    }
                )
            }

            // Tickle
            composable(Screen.Tickle.route) {
                if (useTwoPane) {
                    TicklePane(
                        onCompose = { id, reminderId ->
                            navController.navigate(Screen.Compose.createRoute(id, reminderId)) {
                                launchSingleTop = true
                            }
                        },
                        onAddContact = { navController.navigate(Screen.AddContact.route) },
                    )
                } else {
                    TickleListScreen(
                        onAddTickle = { navController.navigate(Screen.TickleEdit.createRoute(-1L)) },
                        onEditTickle = { id -> navController.navigate(Screen.TickleEdit.createRoute(id)) },
                        onCompose = { id, reminderId ->
                            navController.navigate(Screen.Compose.createRoute(id, reminderId)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.TickleEdit.ROUTE,
                arguments = listOf(
                    navArgument("tickleId") { type = NavType.LongType },
                    navArgument("contactId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("groupId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val tickleId = backStackEntry.arguments?.getLong("tickleId") ?: -1L
                val contactId = backStackEntry.arguments?.getLong("contactId")?.takeIf { it != -1L }
                val groupId = backStackEntry.arguments?.getLong("groupId")?.takeIf { it != -1L }
                TickleEditScreen(
                    tickleId = if (tickleId == -1L) null else tickleId,
                    preselectedContactId = contactId,
                    preselectedGroupId = groupId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onAddContact = { navController.navigate(Screen.AddContact.route) },
                )
            }

            // Compose — accepts optional contactId (pre-selection) and reminderId
            // (TIC-82 mark-done prompt) query params, from ContactDetailScreen,
            // the Tickle action sheet, or a reminder-notification deep link.
            composable(
                route = Screen.Compose.ROUTE,
                arguments = listOf(
                    navArgument(Screen.Compose.ARG_CONTACT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.Compose.ARG_REMINDER_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
                deepLinks = listOf(navDeepLink { uriPattern = Screen.Compose.DEEP_LINK_PATTERN })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong(Screen.Compose.ARG_CONTACT_ID)?.takeIf { it != -1L }
                val reminderId = backStackEntry.arguments?.getLong(Screen.Compose.ARG_REMINDER_ID)?.takeIf { it != -1L }
                ComposeScreen(
                    initialContactId = contactId,
                    initialReminderId = reminderId,
                    // TIC-90: "Manage templates…" (dropdown) / "Create a
                    // template…" (empty state) both land here. Compose stays
                    // on the back stack (no popUpTo), so returning restores
                    // the draft exactly as it was.
                    onManageTemplates = { navController.navigate(Screen.TemplateList.route) },
                    // TIC-96: "Add a number" for a selected contact with no
                    // phone on file. Compose stays on the back stack, so
                    // returning re-reads the (now updated) contact.
                    onEditContact = { id -> navController.navigate("edit_contact/$id") },
                    // TIC-96: "Add or import contacts" when the whole database
                    // is empty — Import wins here since Compose is a bulk
                    // messaging surface, so getting many contacts in fast beats
                    // adding one.
                    onImportContacts = {
                        navController.navigate(Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP))
                    },
                    onDone = {
                        // Flow back to wherever Compose was opened from
                        // (Contact Detail, Tickle list, or the previous tab).
                        // Fallback covers the cold-start deep-link case where
                        // nothing is underneath on the stack.
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Tickle.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onImport = {
                        navController.navigate(Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP))
                    },
                    onTemplates = { navController.navigate(Screen.TemplateList.route) },
                    onResetOnboarding = {
                        prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, false).apply()
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Template management
            composable(Screen.TemplateList.route) {
                TemplateListScreen(
                    onBack = { navController.popBackStack() },
                    onAddTemplate = { navController.navigate(Screen.TemplateEdit.createRoute(-1L)) },
                    onEditTemplate = { id -> navController.navigate(Screen.TemplateEdit.createRoute(id)) }
                )
            }

            composable(
                route = Screen.TemplateEdit.ROUTE,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { backStackEntry ->
                val templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L
                TemplateEditScreen(
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Primary navigation chrome — bottom bar (compact) and side rail (medium/expanded).
// Both drive the same top-level destinations and share selection/navigation logic.
// ---------------------------------------------------------------------------

private fun isTabSelected(currentDestination: NavDestination?, screen: Screen): Boolean =
    currentDestination?.hierarchy?.any {
        it.route?.substringBefore('?')?.substringBefore('/') == screen.route
    } == true

private fun navigateToTab(navController: NavController, screen: Screen) {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppBottomNavBar(
    currentDestination: NavDestination?,
    navController: NavController
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = isTabSelected(currentDestination, item.screen),
                onClick = { navigateToTab(navController, item.screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentDestination: NavDestination?,
    navController: NavController
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelResId)
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = isTabSelected(currentDestination, item.screen),
                onClick = { navigateToTab(navController, item.screen) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
