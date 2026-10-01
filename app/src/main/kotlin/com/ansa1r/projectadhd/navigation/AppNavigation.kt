package com.ansa1r.projectadhd.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.ui.components.LocalCalmSurfaces
import com.ansa1r.projectadhd.ui.mascot.MascotBackdrop
import com.ansa1r.projectadhd.ui.theme.BrandColors
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.apps.AppsScreen
import com.ansa1r.projectadhd.ui.apps.AppsViewModel
import com.ansa1r.projectadhd.ui.components.factory
import com.ansa1r.projectadhd.ui.debug.DebugScreen
import com.ansa1r.projectadhd.ui.debug.DebugViewModel
import com.ansa1r.projectadhd.ui.habits.HabitsScreen
import com.ansa1r.projectadhd.ui.habits.HabitsViewModel
import com.ansa1r.projectadhd.ui.home.HomeScreen
import com.ansa1r.projectadhd.ui.home.HomeViewModel
import com.ansa1r.projectadhd.ui.settings.SettingsScreen
import com.ansa1r.projectadhd.ui.settings.SettingsViewModel
import com.ansa1r.projectadhd.ui.stats.StatsScreen
import com.ansa1r.projectadhd.ui.stats.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(container: AppContainer, habitsRequest: Int = 0) {
    val nav = rememberNavController()
    LaunchedEffect(habitsRequest) {
        if (habitsRequest > 0) nav.navigate(Screen.HABITS.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Screen.HOME.route
    val title = Screen.entries.firstOrNull { it.route == route }?.title ?: R.string.debug
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(if (route == Screen.HOME.route) R.string.app_name else title),
                maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (route == "debug") TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    if (BuildConfig.DEBUG && route != "debug") {
                        TextButton(onClick = { nav.navigate("debug") { launchSingleTop = true } }) { Text(stringResource(R.string.debug)) }
                    }
                })
        },
        bottomBar = {
            NavigationBar(containerColor = BrandColors.Background, tonalElevation = 0.dp) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(selected = route == screen.route,
                        onClick = { nav.navigate(screen.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } },
                        icon = { Icon(painterResource(screen.icon), contentDescription = null) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = BrandColors.PurpleSurface,
                            selectedIconColor = BrandColors.Text, selectedTextColor = BrandColors.Text),
                        label = { Text(stringResource(screen.title), maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
        }
    ) { padding ->
        MascotBackdrop(Modifier.fillMaxSize().padding(padding), enabled = route != Screen.SETTINGS.route) {
            NavHost(navController = nav, startDestination = Screen.HOME.route) {
                composable(Screen.HOME.route) {
                    HomeScreen(viewModel(factory = factory { HomeViewModel(container) }),
                        openHabits = { nav.navigate(Screen.HABITS.route) { launchSingleTop = true } },
                        openApps = { nav.navigate(Screen.APPS.route) { launchSingleTop = true } },
                        openStats = { nav.navigate(Screen.STATS.route) { launchSingleTop = true } })
                }
                composable(Screen.HABITS.route) {
                    HabitsScreen(viewModel(factory = factory { HabitsViewModel(container) }))
                }
                composable(Screen.APPS.route) {
                    AppsScreen(viewModel(factory = factory { AppsViewModel(container) }))
                }
                composable(Screen.STATS.route) {
                    StatsScreen(viewModel(factory = factory { StatsViewModel(container) }))
                }
                composable(Screen.SETTINGS.route) {
                    CompositionLocalProvider(LocalCalmSurfaces provides true) {
                        SettingsScreen(viewModel(factory = factory { SettingsViewModel(container) }))
                    }
                }
                if (BuildConfig.DEBUG) composable("debug") {
                    DebugScreen(viewModel(factory = factory { DebugViewModel(container) }))
                }
            }
        }
    }
}
