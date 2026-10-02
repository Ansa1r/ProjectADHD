package com.ansa1r.projectadhd.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import com.ansa1r.projectadhd.ui.onboarding.*
import com.ansa1r.projectadhd.ui.startup.LocalStartupVisible
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.ui.mascot.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ansa1r.projectadhd.ui.profile.*
import com.ansa1r.projectadhd.ui.settings.*
import com.ansa1r.projectadhd.ui.stats.*
import com.ansa1r.projectadhd.ui.theme.BrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(container: AppContainer, habitsRequest: Int = 0, stopRequest: Int = 0) {
    val nav = rememberNavController()
    var handledHabitsRequest by rememberSaveable { mutableIntStateOf(0) }
    var handledStopRequest by rememberSaveable { mutableIntStateOf(0) }
    var pendingStopRequest by rememberSaveable { mutableIntStateOf(0) }
    val onboardingVm: OnboardingViewModel = viewModel(factory = factory { OnboardingViewModel(container) })
    val onboardingState by onboardingVm.state.collectAsStateWithLifecycle()
    val step = onboardingState?.takeUnless { it.onboardingCompleted }?.onboardingStep
    val guided = onboardingState?.onboardingCompleted != true
    val coachVisible = step != null && !step.isEditor
    val startupVisible = LocalStartupVisible.current
    val profileVm: ProfileViewModel = viewModel(factory = factory { ProfileViewModel(container) })
    val profile by profileVm.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Screen.HOME.route
    val mainSection = Screen.entries.firstOrNull { it.route == route } ?: when {
        route == AppsRoutes.LIMITS -> Screen.APPS
        route.startsWith("habits/") -> Screen.HABITS
        route.startsWith("mascot/") -> Screen.MASCOT
        else -> null
    }
    val settings = SettingsRoutes.isSettings(route)
    val title = when (route) {
        AppsRoutes.LIMITS -> R.string.session_limit_setup
        ProfileRoutes.ROOT -> R.string.profile
        HabitRoutes.CREATE -> R.string.add_habit
        HabitRoutes.EDIT -> R.string.edit_habit
        MascotRoutes.CUSTOMIZE -> R.string.mascot_customization
        MascotRoutes.HISTORY -> R.string.mascot_history
        ProfileRoutes.EDIT -> R.string.edit_profile
        ProfileRoutes.PROGRESS -> R.string.my_progress
        ProfileRoutes.ACHIEVEMENTS -> R.string.achievements
        SettingsRoutes.ROOT -> R.string.settings
        SettingsRoutes.PERMISSIONS -> R.string.permissions_title
        SettingsRoutes.BLOCKING -> R.string.blocking_settings
        SettingsRoutes.THEME -> R.string.interface_theme
        SettingsRoutes.PRIVACY -> R.string.privacy
        SettingsRoutes.DEVELOPER -> R.string.developer
        else -> mainSection?.title ?: R.string.debug
    }
    fun open(destination: String) { nav.navigate(destination) { launchSingleTop = true } }
    fun openMain(screen: Screen) {
        if (nav.currentDestination?.route == screen.route) return
        nav.navigate(if (screen == Screen.APPS) AppsRoutes.ROOT else screen.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
            restoreState = false
        }
    }
    fun back() {
        if (step?.isEditor == true && route != AppsRoutes.LIMITS) onboardingVm.backFromEditor()
        else if (!guided || route == AppsRoutes.LIMITS) nav.popBackStack()
    }
    LaunchedEffect(habitsRequest, guided) { if (habitsRequest > handledHabitsRequest && !guided) {
        handledHabitsRequest = habitsRequest; openMain(Screen.HABITS)
    } }
    LaunchedEffect(stopRequest, guided) { if (stopRequest > handledStopRequest && !guided) {
        handledStopRequest = stopRequest; pendingStopRequest = stopRequest; open(SettingsRoutes.ROOT)
    } }
    LaunchedEffect(step) {
        when (step) {
            OnboardingStep.WELCOME, OnboardingStep.HOME, OnboardingStep.FINAL, OnboardingStep.SETUP_PERMISSIONS -> openMain(Screen.HOME)
            OnboardingStep.HABITS, OnboardingStep.SETUP_HABIT -> openMain(Screen.HABITS)
            OnboardingStep.APPS, OnboardingStep.SETUP_APPS -> openMain(Screen.APPS)
            OnboardingStep.STATS -> openMain(Screen.STATS)
            OnboardingStep.MASCOT -> openMain(Screen.MASCOT)
            OnboardingStep.SELECT_APPS -> if (route != AppsRoutes.SELECTION && route != AppsRoutes.LIMITS) openMain(Screen.APPS)
            OnboardingStep.CREATE_HABIT -> if (route != HabitRoutes.CREATE) { openMain(Screen.HABITS); open(HabitRoutes.CREATE) }
            OnboardingStep.GRANT_PERMISSIONS -> if (route != SettingsRoutes.PERMISSIONS) { openMain(Screen.HOME); open(SettingsRoutes.PERMISSIONS) }
            else -> Unit
        }
    }
    BackHandler(enabled = guided && !coachVisible) { back() }
    if (onboardingState == null) {
        val loadError by onboardingVm.message.collectAsStateWithLifecycle()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadError == null) CircularProgressIndicator()
            else Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Не удалось прочитать настройки. Попробуй ещё раз.")
                TextButton(onboardingVm::reload) { Text("Повторить") }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(modifier = if (coachVisible) Modifier.clearAndSetSemantics { } else Modifier,
        topBar = {
            TopAppBar(title = {
                if (route == Screen.HOME.route) Row(Modifier.clickable(enabled = !guided) { open(ProfileRoutes.ROOT) }.testTag("home_profile"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserAvatar(profile.avatar, Modifier.size(42.dp))
                    Text(profile.nickname ?: stringResource(R.string.nickname_default), maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else if (route != Screen.MASCOT.route) Text(stringResource(title), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }, navigationIcon = {
                if (mainSection == null || route != mainSection.route) IconButton(onClick = { back() }, enabled = !coachVisible) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                }
            }, actions = {
                if (route == Screen.HOME.route) IconButton(onClick = { open(SettingsRoutes.ROOT) }, enabled = !guided) {
                    Icon(painterResource(R.drawable.ic_nav_settings), contentDescription = stringResource(R.string.settings))
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandColors.Background))
        },
        bottomBar = {
            if (mainSection != null) NavigationBar(modifier = Modifier.testTag("bottom_navigation"),
                containerColor = BrandColors.Background, tonalElevation = 0.dp) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(selected = mainSection == screen,
                        modifier = Modifier.testTag("bottom_item_" + screen.route),
                        onClick = { if (!guided) openMain(screen) }, enabled = !guided,
                        icon = { Icon(painterResource(screen.icon), contentDescription = stringResource(screen.title)) },
                        label = null, alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = BrandColors.PurpleSurface.copy(alpha = com.ansa1r.projectadhd.ui.theme.BrandOpacity.Ordinary),
                            selectedIconColor = BrandColors.Text, unselectedIconColor = BrandColors.Muted))
                }
            }
        }
    ) { padding ->
        MascotBackdrop(Modifier.fillMaxSize().padding(padding), enabled = !settings) {
            CompositionLocalProvider(LocalCalmSurfaces provides settings, LocalCoachStep provides step) {
                NavHost(navController = nav, startDestination = Screen.HOME.route) {
                    composable(Screen.HOME.route) {
                        HomeScreen(viewModel(factory = factory { HomeViewModel(container) }),
                            openHabits = { openMain(Screen.HABITS) }, openApps = { openMain(Screen.APPS) },
                            openStats = { openMain(Screen.STATS) }, openPermissions = { open(SettingsRoutes.PERMISSIONS) })
                    }
                    composable(Screen.HABITS.route) { HabitsScreen(viewModel(factory = factory { HabitsViewModel(container) }),
                        create = { open(HabitRoutes.CREATE) }, edit = { open("habits/edit/$it") }) }
                    composable(HabitRoutes.CREATE) { screenEntry -> CreateHabitScreen(viewModel(factory = factory { CreateHabitViewModel(container) }), saved = {
                        if (step == OnboardingStep.CREATE_HABIT) onboardingVm.advance(OnboardingStep.CREATE_HABIT)
                        else if (!guided && nav.currentBackStackEntry?.id == screenEntry.id) nav.popBackStack()
                    }) }
                    composable(HabitRoutes.EDIT) { screenEntry -> CreateHabitScreen(viewModel(factory = factory {
                        CreateHabitViewModel(container, screenEntry.arguments?.getString("id")?.toLongOrNull())
                    }), saved = { if (nav.currentBackStackEntry?.id == screenEntry.id) nav.popBackStack() }) }
                    composable(Screen.MASCOT.route) { MascotScreen(viewModel(factory = factory { MascotViewModel(container) }),
                        customize = { open(MascotRoutes.CUSTOMIZE) }, history = { open(MascotRoutes.HISTORY) }) }
                    composable(MascotRoutes.CUSTOMIZE) { ProfilePlaceholderScreen(R.string.mascot_customization, R.string.mascot_customization_hint) }
                    composable(MascotRoutes.HISTORY) { MascotHistoryScreen(viewModel(factory = factory { MascotViewModel(container) })) }
                    navigation(startDestination = AppsRoutes.SELECTION, route = AppsRoutes.ROOT) {
                        composable(AppsRoutes.SELECTION) { screenEntry ->
                            val owner = remember(screenEntry) { nav.getBackStackEntry(AppsRoutes.ROOT) }
                            val vm: AppsViewModel = viewModel(viewModelStoreOwner = owner, factory = factory { AppsViewModel(container) })
                            AppsScreen(vm, continueToLimits = { open(AppsRoutes.LIMITS) }, saved = {
                                if (step == OnboardingStep.SELECT_APPS) onboardingVm.advance(OnboardingStep.SELECT_APPS)
                                else if (!guided && nav.currentBackStackEntry?.id == screenEntry.id) openMain(Screen.HOME)
                            })
                        }
                        composable(AppsRoutes.LIMITS) { screenEntry ->
                            val owner = remember(screenEntry) { nav.getBackStackEntry(AppsRoutes.ROOT) }
                            val vm: AppsViewModel = viewModel(viewModelStoreOwner = owner, factory = factory { AppsViewModel(container) })
                            SessionLimitScreen(vm, saved = {
                                if (step == OnboardingStep.SELECT_APPS) onboardingVm.advance(OnboardingStep.SELECT_APPS)
                                else if (!guided && nav.currentBackStackEntry?.id == screenEntry.id) openMain(Screen.HOME)
                            }, returnToSelection = { nav.popBackStack() })
                        }
                    }
                    composable(Screen.STATS.route) { StatsScreen(viewModel(factory = factory { StatsViewModel(container) })) }
                    composable(ProfileRoutes.ROOT) {
                        ProfileScreen(profileVm,
                            openProgress = { open(ProfileRoutes.PROGRESS) },
                            editProfile = { open(ProfileRoutes.EDIT) })
                    }
                    composable(ProfileRoutes.EDIT) { screenEntry ->
                        EditProfileScreen(viewModel(factory = factory { EditProfileViewModel(container) }), saved = {
                            if (nav.currentBackStackEntry?.id == screenEntry.id) nav.popBackStack()
                        })
                    }
                    composable(ProfileRoutes.PROGRESS) { StatsScreen(viewModel(factory = factory { StatsViewModel(container) })) }
                    composable(ProfileRoutes.ACHIEVEMENTS) { ProfilePlaceholderScreen(R.string.achievements, R.string.achievements_placeholder) }
                    composable(SettingsRoutes.ROOT) {
                        SettingsScreen(openPermissions = { open(SettingsRoutes.PERMISSIONS) },
                            openBlocking = { open(SettingsRoutes.BLOCKING) }, openTheme = { open(SettingsRoutes.THEME) },
                            openPrivacy = { open(SettingsRoutes.PRIVACY) }, openDeveloper = { open(SettingsRoutes.DEVELOPER) },
                            monitoringViewModel = viewModel(factory = factory { MonitoringSettingsViewModel(container) }), stopRequest = pendingStopRequest,
                            stopRequestHandled = { pendingStopRequest = 0 })
                    }
                    composable(SettingsRoutes.PERMISSIONS) { PermissionsScreen(viewModel(factory = factory { PermissionsViewModel(container) }),
                        onReady = onboardingVm::refreshPermissions) }
                    composable(SettingsRoutes.BLOCKING) {
                        BlockingSettingsScreen(viewModel(factory = factory { SettingsViewModel(container) }))
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
    if (coachVisible && !startupVisible) OnboardingCoach(onboardingVm, requireNotNull(step))
    }
    if (!guided && route != SettingsRoutes.ROOT) PendingHabitConfirmation(container, inlineOnHabits = route == Screen.HABITS.route)
}
