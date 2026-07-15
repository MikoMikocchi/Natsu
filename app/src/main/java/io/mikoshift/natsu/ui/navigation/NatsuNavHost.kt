package io.mikoshift.natsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.mikoshift.natsu.feature.auth.authGraph
import io.mikoshift.natsu.feature.library.libraryGraph
import io.mikoshift.natsu.feature.profile.profileGraph
import io.mikoshift.natsu.navigation.ChangePasswordRoute
import io.mikoshift.natsu.navigation.HomeRoute
import io.mikoshift.natsu.navigation.LoginRoute
import io.mikoshift.natsu.navigation.ProfileRoute
import io.mikoshift.natsu.navigation.RegisterRoute

@Composable
fun NatsuNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val session by rootViewModel.session.collectAsStateWithLifecycle()
    val startDestination = if (session != null) HomeRoute else LoginRoute

    LaunchedEffect(session) {
        if (session == null) {
            rootViewModel.onSessionCleared()
        }

        val destination = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect

        when {
            session == null && (
                destination.hasRoute<HomeRoute>() ||
                    destination.hasRoute<ProfileRoute>() ||
                    destination.hasRoute<ChangePasswordRoute>()
                ) -> {
                navController.navigate(LoginRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            session != null && (
                destination.hasRoute<LoginRoute>() ||
                    destination.hasRoute<RegisterRoute>()
                ) -> {
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
