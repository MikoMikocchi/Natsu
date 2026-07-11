package io.mikoshift.natsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.mikoshift.natsu.di.AppContainer
import io.mikoshift.natsu.ui.auth.AuthViewModelFactory
import io.mikoshift.natsu.ui.auth.LoginScreen
import io.mikoshift.natsu.ui.auth.LoginViewModel
import io.mikoshift.natsu.ui.auth.RegisterScreen
import io.mikoshift.natsu.ui.auth.RegisterViewModel
import io.mikoshift.natsu.ui.home.HomeScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_HOME = "home"

@Composable
fun NatsuNavHost(appContainer: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val authViewModelFactory = AuthViewModelFactory(appContainer.authRepository)
    val startDestination = if (appContainer.authRepository.currentSession.value != null) {
        ROUTE_HOME
    } else {
        ROUTE_LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = authViewModelFactory)
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) },
                onLoggedIn = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                        popUpTo(ROUTE_REGISTER) { inclusive = true }
                    }
                },
            )
        }

        composable(ROUTE_REGISTER) {
            val viewModel: RegisterViewModel = viewModel(factory = authViewModelFactory)
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(ROUTE_LOGIN) },
                onRegistered = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                        popUpTo(ROUTE_REGISTER) { inclusive = true }
                    }
                },
            )
        }

        composable(ROUTE_HOME) {
            HomeScreen(
                authRepository = appContainer.authRepository,
                onLoggedOut = {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
