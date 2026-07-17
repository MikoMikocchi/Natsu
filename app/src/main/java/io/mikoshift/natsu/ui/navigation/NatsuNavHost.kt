package io.mikoshift.natsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.mikoshift.natsu.feature.auth.authGraph
import io.mikoshift.natsu.feature.library.libraryGraph
import io.mikoshift.natsu.feature.profile.profileGraph
import io.mikoshift.natsu.feature.reader.readerGraph
import io.mikoshift.natsu.navigation.HomeRoute
import io.mikoshift.natsu.navigation.LoginRoute
import io.mikoshift.natsu.navigation.SessionRedirect
import io.mikoshift.natsu.navigation.resolveSessionRedirect
import io.mikoshift.natsu.navigation.startDestinationForSession

@Composable
fun NatsuNavHost(
    deepLinkTrigger: Int = 0,
    sessionHost: SessionHost = hiltViewModel<RootViewModel>(),
    navGraphBuilder: NavGraphBuilder.(NavHostController) -> Unit = { navController ->
        authGraph(navController)
        libraryGraph(navController)
        profileGraph(navController)
        readerGraph(navController)
    },
) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val session by sessionHost.session.collectAsStateWithLifecycle()
    val startDestination = startDestinationForSession(session != null)

    LaunchedEffect(deepLinkTrigger) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        navController.handleDeepLink(activity.intent)
    }

    LaunchedEffect(session) {
        if (session == null) {
            sessionHost.onSessionCleared()
        }

        when (resolveSessionRedirect(hasSession = session != null, destination = navController.currentBackStackEntry?.destination)) {
            SessionRedirect.ToLogin -> {
                navController.navigate(LoginRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            SessionRedirect.ToHome -> {
                navController.navigate(HomeRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
            null -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        navGraphBuilder(navController)
    }
}
