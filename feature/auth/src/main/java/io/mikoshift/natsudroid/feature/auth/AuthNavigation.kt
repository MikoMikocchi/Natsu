package io.mikoshift.natsudroid.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import io.mikoshift.natsudroid.navigation.ForgotPasswordRoute
import io.mikoshift.natsudroid.navigation.LoginRoute
import io.mikoshift.natsudroid.navigation.RegisterRoute
import io.mikoshift.natsudroid.navigation.ResetPasswordRoute
import io.mikoshift.natsudroid.ui.auth.ForgotPasswordScreen
import io.mikoshift.natsudroid.ui.auth.ForgotPasswordViewModel
import io.mikoshift.natsudroid.ui.auth.LoginScreen
import io.mikoshift.natsudroid.ui.auth.LoginViewModel
import io.mikoshift.natsudroid.ui.auth.RegisterScreen
import io.mikoshift.natsudroid.ui.auth.RegisterViewModel
import io.mikoshift.natsudroid.ui.auth.ResetPasswordScreen
import io.mikoshift.natsudroid.ui.auth.ResetPasswordViewModel

class AuthViewModelProviders(
    val login: @Composable () -> LoginViewModel,
    val register: @Composable () -> RegisterViewModel,
    val forgotPassword: @Composable () -> ForgotPasswordViewModel,
    val resetPassword: @Composable () -> ResetPasswordViewModel,
)

fun NavGraphBuilder.authGraph(navController: NavHostController, viewModels: AuthViewModelProviders) {
    composable<LoginRoute> {
        LoginScreen(
            viewModel = viewModels.login(),
            onNavigateToRegister = { navController.navigate(RegisterRoute) },
            onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) },
        )
    }

    composable<RegisterRoute> {
        val viewModel = viewModels.register()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(uiState.registrationSucceeded) {
            if (uiState.registrationSucceeded) {
                navController.navigate(LoginRoute) {
                    popUpTo(RegisterRoute) { inclusive = true }
                }
            }
        }
        RegisterScreen(
            viewModel = viewModel,
            onNavigateToLogin = { navController.navigate(LoginRoute) },
        )
    }

    composable<ForgotPasswordRoute> {
        ForgotPasswordScreen(
            viewModel = viewModels.forgotPassword(),
            onNavigateToLogin = { navController.navigate(LoginRoute) },
        )
    }

    composable<ResetPasswordRoute>(
        deepLinks =
        listOf(
            navDeepLink<ResetPasswordRoute>(
                basePath = "https://natsu.mikoshift.io/reset-password",
            ),
            navDeepLink<ResetPasswordRoute>(
                basePath = "natsudroid://reset-password",
            ),
        ),
    ) {
        ResetPasswordScreen(
            viewModel = viewModels.resetPassword(),
            onNavigateToLogin = {
                navController.navigate(LoginRoute) {
                    popUpTo(ForgotPasswordRoute) { inclusive = true }
                }
            },
        )
    }
}
