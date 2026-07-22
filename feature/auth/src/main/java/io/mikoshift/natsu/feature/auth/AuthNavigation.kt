package io.mikoshift.natsu.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import io.mikoshift.natsu.navigation.ForgotPasswordRoute
import io.mikoshift.natsu.navigation.LoginRoute
import io.mikoshift.natsu.navigation.RegisterRoute
import io.mikoshift.natsu.navigation.ResetPasswordRoute
import io.mikoshift.natsu.ui.auth.ForgotPasswordScreen
import io.mikoshift.natsu.ui.auth.ForgotPasswordViewModel
import io.mikoshift.natsu.ui.auth.LoginScreen
import io.mikoshift.natsu.ui.auth.LoginViewModel
import io.mikoshift.natsu.ui.auth.RegisterScreen
import io.mikoshift.natsu.ui.auth.RegisterViewModel
import io.mikoshift.natsu.ui.auth.ResetPasswordScreen
import io.mikoshift.natsu.ui.auth.ResetPasswordViewModel

class AuthViewModelProviders(
    val login: @Composable () -> LoginViewModel,
    val register: @Composable () -> RegisterViewModel,
    val forgotPassword: @Composable () -> ForgotPasswordViewModel,
    val resetPassword: @Composable () -> ResetPasswordViewModel,
)

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    viewModels: AuthViewModelProviders,
) {
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
                basePath = "natsu://reset-password",
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
