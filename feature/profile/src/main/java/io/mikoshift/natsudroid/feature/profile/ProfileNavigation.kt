package io.mikoshift.natsudroid.feature.profile

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsudroid.navigation.ChangePasswordRoute
import io.mikoshift.natsudroid.navigation.ProfileRoute
import io.mikoshift.natsudroid.ui.profile.ChangePasswordScreen
import io.mikoshift.natsudroid.ui.profile.ChangePasswordViewModel
import io.mikoshift.natsudroid.ui.profile.ProfileScreen
import io.mikoshift.natsudroid.ui.profile.ProfileViewModel

class ProfileViewModelProviders(
    val profile: @Composable () -> ProfileViewModel,
    val changePassword: @Composable () -> ChangePasswordViewModel,
)

fun NavGraphBuilder.profileGraph(navController: NavHostController, viewModels: ProfileViewModelProviders) {
    composable<ProfileRoute> {
        ProfileScreen(
            viewModel = viewModels.profile(),
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChangePassword = { navController.navigate(ChangePasswordRoute) },
        )
    }

    composable<ChangePasswordRoute> {
        ChangePasswordScreen(
            viewModel = viewModels.changePassword(),
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
