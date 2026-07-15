package io.mikoshift.natsu.feature.auth

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
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

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<LoginRoute> {
        val viewModel: LoginViewModel = hiltViewModel()
        LoginScreen(
            viewModel = viewModel,
            onNavigateToRegister = { navController.navigate(RegisterRoute) },
            onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) },
        )
    }

    composable<RegisterRoute> {
        val viewModel: RegisterViewModel = hiltViewModel()
        RegisterScreen(
            viewModel = viewModel,
            onNavigateToLogin = { navController.navigate(LoginRoute) },
        )
    }

    composable<ForgotPasswordRoute> {
        val viewModel: ForgotPasswordViewModel = hiltViewModel()
        ForgotPasswordScreen(
            viewModel = viewModel,
            onNavigateToLogin = { navController.navigate(LoginRoute) },
        )
    }

    composable<ResetPasswordRoute> {
        val viewModel: ResetPasswordViewModel = hiltViewModel()
        ResetPasswordScreen(
            viewModel = viewModel,
            onNavigateToLogin = {
                navController.navigate(LoginRoute) {
                    popUpTo(ForgotPasswordRoute) { inclusive = true }
                }
            },
        )
    }
}
