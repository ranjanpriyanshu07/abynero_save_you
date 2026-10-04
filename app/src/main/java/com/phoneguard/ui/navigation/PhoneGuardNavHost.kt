package com.phoneguard.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phoneguard.ui.screens.appdetail.AppDetailScreen
import com.phoneguard.ui.screens.appdetail.AppDetailViewModel
import com.phoneguard.ui.screens.history.HistoryScreen
import com.phoneguard.ui.screens.home.HomeScreen
import com.phoneguard.ui.screens.onboarding.OnboardingScreen
import com.phoneguard.ui.screens.onboarding.PermissionsScreen
import com.phoneguard.ui.screens.scan.ScanScreen
import com.phoneguard.ui.screens.tips.TipsScreen
import com.phoneguard.ui.screens.tools.LinkCheckScreen
import com.phoneguard.ui.screens.tools.QrScannerScreen
import com.phoneguard.ui.screens.tools.ToolsScreen
import com.phoneguard.ui.screens.tools.WifiCheckScreen

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
private const val ANIM_MS = 320

/**
 * Root of the UI: bottom navigation bar plus the navigation graph with smooth transitions.
 *
 * @param startWithOnboarding true on first launch.
 */
@Composable
fun PhoneGuardRoot(
    startWithOnboarding: Boolean,
    navigationRequests: NavigationRequests,
) {
    val navController = rememberNavController()
    val startDestination = remember { if (startWithOnboarding) Routes.ONBOARDING else Routes.HOME }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes
    val pending by navigationRequests.pending.collectAsStateWithLifecycle()

    LaunchedEffect(pending, currentRoute) {
        val request = pending ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == Routes.ONBOARDING || currentRoute == Routes.PERMISSIONS) {
            return@LaunchedEffect
        }
        when (request) {
            is PendingNavigation.SharedText -> {
                navController.navigate(Routes.linkCheck())
                navController.currentBackStackEntry?.savedStateHandle?.set(Routes.KEY_INCOMING_TEXT, request.text)
            }
            PendingNavigation.OpenScan -> navController.navigateToTab(Routes.SCAN)
            is PendingNavigation.OpenApp -> navController.navigate(Routes.appDetail(request.packageName))
        }
        navigationRequests.consume()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination.route) },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            enterTransition = { enterFor(this) },
            exitTransition = { exitFor(this) },
            popEnterTransition = { fadeIn(tween(ANIM_MS)) },
            popExitTransition = { popExitFor(this) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinished = { navController.navigate(Routes.PERMISSIONS) })
            }
            composable(Routes.PERMISSIONS) {
                PermissionsScreen(
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenResults = { navController.navigateToTab(Routes.SCAN) },
                    onOpenWifi = { navController.navigate(Routes.WIFI) },
                    onOpenLinks = { navController.navigate(Routes.linkCheck()) },
                    onOpenTips = { navController.navigateToTab(Routes.TIPS) },
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onOpenApp = { navController.navigate(Routes.appDetail(it)) },
                    onOpenWifi = { navController.navigate(Routes.WIFI) },
                )
            }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    onOpenLinkCheck = { navController.navigate(Routes.linkCheck()) },
                    onOpenQrScanner = { navController.navigate(Routes.linkCheck(openQr = true)) },
                    onOpenWifi = { navController.navigate(Routes.WIFI) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onScanStarted = { navController.navigateToTab(Routes.SCAN) })
            }
            composable(Routes.TIPS) { TipsScreen() }
            composable(
                route = Routes.APP_DETAIL,
                arguments = listOf(navArgument(AppDetailViewModel.ARG_PACKAGE_NAME) { type = NavType.StringType }),
            ) {
                AppDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.LINK_CHECK,
                arguments = listOf(
                    navArgument(Routes.ARG_OPEN_QR) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                val incoming by entry.savedStateHandle
                    .getStateFlow<String?>(Routes.KEY_INCOMING_TEXT, null)
                    .collectAsStateWithLifecycle()
                LinkCheckScreen(
                    incomingText = incoming,
                    onIncomingConsumed = { entry.savedStateHandle[Routes.KEY_INCOMING_TEXT] = null },
                    openQrOnStart = entry.arguments?.getBoolean(Routes.ARG_OPEN_QR) ?: false,
                    onOpenQr = { navController.navigate(Routes.QR_SCANNER) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.QR_SCANNER) {
                QrScannerScreen(
                    onResult = { value ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.KEY_INCOMING_TEXT, value)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.WIFI) { WifiCheckScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

/** Switches tabs, keeping one copy of each tab and restoring its scroll state. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun isTabSwitch(scope: AnimatedContentTransitionScope<NavBackStackEntry>): Boolean =
    scope.initialState.destination.route in topLevelRoutes && scope.targetState.destination.route in topLevelRoutes

private fun enterFor(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition =
    if (isTabSwitch(scope)) {
        fadeIn(tween(ANIM_MS))
    } else {
        fadeIn(tween(ANIM_MS)) + scope.slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            tween(ANIM_MS),
            initialOffset = { it / 8 },
        )
    }

private fun exitFor(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition =
    fadeOut(tween(if (isTabSwitch(scope)) ANIM_MS / 2 else ANIM_MS))

private fun popExitFor(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition =
    fadeOut(tween(ANIM_MS)) + scope.slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        tween(ANIM_MS),
        targetOffset = { it / 8 },
    )
