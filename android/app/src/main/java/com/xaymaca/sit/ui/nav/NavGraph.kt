package com.xaymaca.sit.ui.nav

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
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

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Network, "Network", Icons.Default.People),
    BottomNavItem(Screen.Tickle, "Tickle", Icons.Default.Notifications),
    BottomNavItem(Screen.GroupList, "Groups", Icons.Default.Group),
    BottomNavItem(Screen.Compose, "Compose", Icons.Default.Email),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

private val bottomNavRoutes = setOf(
    Screen.Network.route,
    Screen.Tickle.route,
    Screen.GroupList.route,
    Screen.Compose.route,
    Screen.Settings.route
)

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
    val onboardingComplete = prefs.getBoolean(SITApp.KEY_ONBOARDING_COMPLETE, false)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
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
        }
    ) { innerPadding ->
        val startDestination = if (onboardingComplete) Screen.Network.route else Screen.Onboarding.route

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onImportContacts = { navController.navigate(Screen.Import.route) },
                    onStartEmpty = {
                        prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).apply()
                        navController.navigate(Screen.Network.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Import
            composable(Screen.Import.route) {
                ImportScreen(
                    onComplete = {
                        prefs.edit().putBoolean(SITApp.KEY_ONBOARDING_COMPLETE, true).apply()
                        navController.navigate(Screen.Network.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Network
            composable(Screen.Network.route) {
                NetworkListScreen(
                    onContactClick = { id ->
                        navController.navigate(Screen.ContactDetail.createRoute(id))
                    },
                    onAddContact = { navController.navigate(Screen.AddContact.route) },
                    onImport = { navController.navigate(Screen.Import.route) }
                )
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
                    onEdit = { navController.navigate("edit_contact/$contactId") }
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
                GroupListScreen(
                    onGroupClick = { id -> navController.navigate(Screen.GroupDetail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.GroupDetail.ROUTE,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
                GroupDetailScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onContactClick = { id ->
                        navController.navigate(Screen.ContactDetail.createRoute(id))
                    }
                )
            }

            // Tickle
            composable(Screen.Tickle.route) {
                TickleListScreen(
                    onAddTickle = { navController.navigate(Screen.TickleEdit.createRoute(-1L)) },
                    onEditTickle = { id -> navController.navigate(Screen.TickleEdit.createRoute(id)) }
                )
            }

            composable(
                route = Screen.TickleEdit.ROUTE,
                arguments = listOf(
                    navArgument("tickleId") { type = NavType.LongType },
                    navArgument("contactId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val tickleId = backStackEntry.arguments?.getLong("tickleId") ?: -1L
                val contactId = backStackEntry.arguments?.getLong("contactId")?.takeIf { it != -1L }
                TickleEditScreen(
                    tickleId = if (tickleId == -1L) null else tickleId,
                    preselectedContactId = contactId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // Compose
            composable(Screen.Compose.route) {
                ComposeScreen(
                    onNavigateToNetwork = {
                        navController.navigate(Screen.Network.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onImport = { navController.navigate(Screen.Import.route) },
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
