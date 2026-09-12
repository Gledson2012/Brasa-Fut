package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlobalSearchBar
import com.example.ui.viewmodels.GlobalSearchViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ThemePreferences
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StandingsScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Início", Icons.Filled.Home)
    object Search : Screen("search", "Busca", Icons.Filled.Search)
    object Matches : Screen("matches", "Partidas", Icons.Filled.SportsSoccer)
    object Calendar : Screen("calendar", "Calendário", Icons.Filled.CalendarMonth)
    object Standings : Screen("standings", "Tabela", Icons.Filled.List)
    object Favorites : Screen("favorites", "Salvos", Icons.Filled.Favorite)
}

val items = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Matches,
    Screen.Calendar,
    Screen.Standings,
    Screen.Favorites
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, themePreferences: ThemePreferences) {
    val navController = rememberNavController()
    
    val globalSearchViewModel: GlobalSearchViewModel = viewModel()
    val searchQuery by globalSearchViewModel.searchQuery.collectAsState()
    val isSearchActive by globalSearchViewModel.isSearchActive.collectAsState()
    val searchUiState by globalSearchViewModel.uiState.collectAsState()

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) {}
        androidx.compose.runtime.LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "settings" && currentRoute != "user_profile" && !(currentRoute?.startsWith("match_details/") == true) && !(currentRoute?.startsWith("team_performance/") == true) && currentRoute != "team_notifications") {
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = globalSearchViewModel::onSearchQueryChanged,
                    active = isSearchActive,
                    onActiveChange = globalSearchViewModel::onSearchActiveChanged,
                    uiState = searchUiState,
                    onMatchSelected = { matchId ->
                        navController.navigate("match_details/$matchId")
                    },
                    onTeamSelected = { teamId ->
                        navController.navigate("team_performance/$teamId")
                    },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "settings" && currentRoute != "user_profile") {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onMatchSelected = { matchId ->
                        navController.navigate("match_details/$matchId")
                    }
                ) 
            }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Matches.route) { 
                MatchesScreen(
                    onMatchSelected = { matchId ->
                        navController.navigate("match_details/$matchId")
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onMatchSelected = { matchId ->
                        navController.navigate("match_details/$matchId")
                    }
                )
            }
            composable(Screen.Standings.route) { 
                StandingsScreen(
                    onTeamSelected = { teamId ->
                        navController.navigate("team_performance/$teamId")
                    }
                )
            }
            composable(
                route = "team_performance/{teamId}",
                arguments = listOf(androidx.navigation.navArgument("teamId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getInt("teamId") ?: 0
                com.example.ui.screens.TeamPerformanceScreen(
                    teamId = teamId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Favorites.route) { 
                FavoritesScreen(
                    onMatchSelected = { matchId ->
                        navController.navigate("match_details/$matchId")
                    }
                ) 
            }
            composable("settings") {
                SettingsScreen(
                    themePreferences = themePreferences,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUserProfile = { navController.navigate("user_profile") },
                    onNavigateToTeamNotifications = { navController.navigate("team_notifications") }
                )
            }
            composable("user_profile") {
                val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.BrasaFutApplication
                val viewModel: com.example.ui.viewmodels.UserProfileViewModel = viewModel(factory = com.example.ui.viewmodels.UserProfileViewModel.Factory(app.userProfilePreferences))
                com.example.ui.screens.UserProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("team_notifications") {
                com.example.ui.screens.TeamNotificationsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("match_details/{matchId}") { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId")?.toIntOrNull() ?: return@composable
                com.example.ui.screens.MatchDetailsScreen(
                    matchId = matchId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
