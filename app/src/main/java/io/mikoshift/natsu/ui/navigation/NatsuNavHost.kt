package io.mikoshift.natsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.mikoshift.natsu.feature.auth.authGraph
import io.mikoshift.natsu.feature.library.libraryGraph
import io.mikoshift.natsu.feature.profile.profileGraph
import io.mikoshift.natsu.navigation.HomeRoute
import io.mikoshift.natsu.navigation.LoginRoute
import io.mikoshift.natsu.navigation.matchesAuthOnlyRoute
import io.mikoshift.natsu.navigation.matchesAuthenticatedRoute

@Composable
fun NatsuNavHost(
    deepLinkTrigger: Int = 0,
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val session by rootViewModel.session.collectAsStateWithLifecycle()
    val startDestination = if (session != null) HomeRoute else LoginRoute

    LaunchedEffect(deepLinkTrigger) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        navController.handleDeepLink(activity.intent)
    }

    LaunchedEffect(session) {
        if (session == null) {
            rootViewModel.onSessionCleared()
        }

        val destination = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect

        when {
            session == null && destination.matchesAuthenticatedRoute() -> {
                navController.navigate(LoginRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            session != null && destination.matchesAuthOnlyRoute() -> {
                navController.navigate(HomeRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(navController)
        libraryGraph(navController)
        profileGraph(navController)
    }
}
