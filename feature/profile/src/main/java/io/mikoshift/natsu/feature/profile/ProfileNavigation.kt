package io.mikoshift.natsu.feature.profile

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsu.navigation.ChangePasswordRoute
import io.mikoshift.natsu.navigation.ProfileRoute
import io.mikoshift.natsu.ui.profile.ChangePasswordScreen
import io.mikoshift.natsu.ui.profile.ChangePasswordViewModel
import io.mikoshift.natsu.ui.profile.ProfileScreen
import io.mikoshift.natsu.ui.profile.ProfileViewModel

fun NavGraphBuilder.profileGraph(navController: NavHostController) {
    composable<ProfileRoute> {
        val viewModel: ProfileViewModel = hiltViewModel()
        ProfileScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChangePassword = { navController.navigate(ChangePasswordRoute) },
        )
    }

    composable<ChangePasswordRoute> {
        val viewModel: ChangePasswordViewModel = hiltViewModel()
        ChangePasswordScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
