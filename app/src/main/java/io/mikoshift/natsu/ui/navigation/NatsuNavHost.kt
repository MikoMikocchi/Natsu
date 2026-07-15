package io.mikoshift.natsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.mikoshift.natsu.ui.auth.ForgotPasswordScreen
import io.mikoshift.natsu.ui.auth.ForgotPasswordViewModel
import io.mikoshift.natsu.ui.auth.LoginScreen
import io.mikoshift.natsu.ui.auth.LoginViewModel
import io.mikoshift.natsu.ui.auth.RegisterScreen
import io.mikoshift.natsu.ui.auth.RegisterViewModel
import io.mikoshift.natsu.ui.auth.ResetPasswordScreen
import io.mikoshift.natsu.ui.auth.ResetPasswordViewModel
import io.mikoshift.natsu.ui.library.LibraryScreen
import io.mikoshift.natsu.ui.library.LibraryViewModel
import io.mikoshift.natsu.ui.profile.ChangePasswordScreen
import io.mikoshift.natsu.ui.profile.ChangePasswordViewModel
import io.mikoshift.natsu.ui.profile.ProfileScreen
import io.mikoshift.natsu.ui.profile.ProfileViewModel

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_FORGOT_PASSWORD = "forgot_password"
private const val ROUTE_RESET_PASSWORD = "reset_password?token={token}"
private const val ROUTE_HOME = "home"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_CHANGE_PASSWORD = "change_password"

private val AUTH_ONLY_ROUTES = setOf(ROUTE_LOGIN, ROUTE_REGISTER)
private val AUTHENTICATED_ROUTES = setOf(ROUTE_HOME, ROUTE_PROFILE, ROUTE_CHANGE_PASSWORD)

@Composable
fun NatsuNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val session by rootViewModel.session.collectAsStateWithLifecycle()
    val startDestination = if (session != null) ROUTE_HOME else ROUTE_LOGIN

    LaunchedEffect(session) {
        if (session == null) {
            rootViewModel.onSessionCleared()
        }

        val currentRoute = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect

        when {
            session == null && currentRoute in AUTHENTICATED_ROUTES -> {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(ROUTE_HOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
            session != null && currentRoute in AUTH_ONLY_ROUTES -> {
                navController.navigate(ROUTE_HOME) {
                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(ROUTE_FORGOT_PASSWORD) },
            )
        }

        composable(ROUTE_REGISTER) {
            val viewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(ROUTE_LOGIN) },
            )
        }

        composable(ROUTE_FORGOT_PASSWORD) {
            val viewModel: ForgotPasswordViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(ROUTE_LOGIN) },
            )
        }

        composable(
            route = ROUTE_RESET_PASSWORD,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            val viewModel: ResetPasswordViewModel = hiltViewModel()
            ResetPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_FORGOT_PASSWORD) { inclusive = true }
                    }
                },
            )
        }

        composable(ROUTE_HOME) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToProfile = { navController.navigate(ROUTE_PROFILE) },
            )
        }

        composable(ROUTE_PROFILE) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(ROUTE_CHANGE_PASSWORD) },
            )
        }

        composable(ROUTE_CHANGE_PASSWORD) {
            val viewModel: ChangePasswordViewModel = hiltViewModel()
            ChangePasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
