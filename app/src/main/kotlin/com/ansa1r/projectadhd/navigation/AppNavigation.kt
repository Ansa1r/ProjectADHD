package com.ansa1r.projectadhd.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.apps.*
import com.ansa1r.projectadhd.ui.components.LocalCalmSurfaces
import com.ansa1r.projectadhd.ui.components.factory
import com.ansa1r.projectadhd.ui.debug.*
import com.ansa1r.projectadhd.ui.habits.*
import com.ansa1r.projectadhd.ui.home.*
import com.ansa1r.projectadhd.ui.mascot.MascotBackdrop
import com.ansa1r.projectadhd.ui.profile.ProfileScreen
import com.ansa1r.projectadhd.ui.settings.*
import com.ansa1r.projectadhd.ui.stats.*
import com.ansa1r.projectadhd.ui.theme.BrandColors

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
    val mainScreen = Screen.entries.firstOrNull { it.route == route }
    val settings = SettingsRoutes.isSettings(route)
    val title = mainScreen?.title ?: when (route) {
        SettingsRoutes.ROOT -> R.string.settings
        SettingsRoutes.PERMISSIONS -> R.string.permissions_title
        SettingsRoutes.BLOCKING -> R.string.blocking_settings
        SettingsRoutes.THEME -> R.string.interface_theme
        SettingsRoutes.PRIVACY -> R.string.privacy
        SettingsRoutes.DEVELOPER -> R.string.developer
        else -> R.string.debug
    }
    fun open(destination: String) { nav.navigate(destination) { launchSingleTop = true } }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(if (route == Screen.HOME.route) R.string.app_name else title),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }, navigationIcon = {
                if (mainScreen == null) IconButton(onClick = { nav.popBackStack() }) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                }
            }, actions = {
                if (route == Screen.HOME.route) IconButton(onClick = { open(SettingsRoutes.ROOT) }) {
                    Icon(painterResource(R.drawable.ic_nav_settings), contentDescription = stringResource(R.string.settings))
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandColors.Background))
        },
        bottomBar = {
            if (mainScreen != null) NavigationBar(modifier = Modifier.testTag("bottom_navigation"),
                containerColor = BrandColors.Background, tonalElevation = 0.dp) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(selected = route == screen.route,
                        modifier = Modifier.testTag("bottom_item_" + screen.route),
                        onClick = { nav.navigate(screen.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } },
                        icon = { Icon(painterResource(screen.icon), contentDescription = stringResource(screen.title)) },
                        label = null, alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = BrandColors.PurpleSurface,
                            selectedIconColor = BrandColors.Text, unselectedIconColor = BrandColors.Muted))
                }
            }
        }
    ) { padding ->
        MascotBackdrop(Modifier.fillMaxSize().padding(padding), enabled = !settings) {
            CompositionLocalProvider(LocalCalmSurfaces provides settings) {
                NavHost(navController = nav, startDestination = Screen.HOME.route) {
                    composable(Screen.HOME.route) {
                        HomeScreen(viewModel(factory = factory { HomeViewModel(container) }),
                            openHabits = { open(Screen.HABITS.route) }, openApps = { open(Screen.APPS.route) },
                            openStats = { open(Screen.STATS.route) }, openPermissions = { open(SettingsRoutes.PERMISSIONS) })
                    }
                    composable(Screen.HABITS.route) { HabitsScreen(viewModel(factory = factory { HabitsViewModel(container) })) }
                    composable(Screen.APPS.route) { AppsScreen(viewModel(factory = factory { AppsViewModel(container) })) }
                    composable(Screen.STATS.route) { StatsScreen(viewModel(factory = factory { StatsViewModel(container) })) }
                    composable(Screen.PROFILE.route) { ProfileScreen() }
                    composable(SettingsRoutes.ROOT) {
                        SettingsScreen(openPermissions = { open(SettingsRoutes.PERMISSIONS) },
                            openBlocking = { open(SettingsRoutes.BLOCKING) }, openTheme = { open(SettingsRoutes.THEME) },
                            openPrivacy = { open(SettingsRoutes.PRIVACY) }, openDeveloper = { open(SettingsRoutes.DEVELOPER) },
                            stopMonitoring = container.controller::stop)
                    }
                    composable(SettingsRoutes.PERMISSIONS) { PermissionsScreen(viewModel(factory = factory { PermissionsViewModel(container) })) }
                    composable(SettingsRoutes.BLOCKING) {
                        BlockingSettingsScreen(viewModel(factory = factory { BlockingSettingsViewModel(container) }),
                            viewModel(factory = factory { SettingsViewModel(container) }))
                    }
                    composable(SettingsRoutes.THEME) { SettingsPlaceholderScreen(R.string.interface_theme, R.string.theme_placeholder) }
                    composable(SettingsRoutes.PRIVACY) { SettingsPlaceholderScreen(R.string.privacy, R.string.privacy_placeholder, showPrivacyInfo = true) }
                    if (BuildConfig.DEBUG) {
                        composable(SettingsRoutes.DEVELOPER) { DeveloperScreen { open(SettingsRoutes.DEBUG) } }
                        composable(SettingsRoutes.DEBUG) { DebugScreen(viewModel(factory = factory { DebugViewModel(container) })) }
                    }
                }
            }
        }
    }
}
