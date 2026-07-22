package io.mikoshift.natsudroid.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import io.mikoshift.natsudroid.di.viewmodel.HiltChangePasswordViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltForgotPasswordViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltLibraryViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltLoginViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltProfileViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltReaderViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltRegisterViewModel
import io.mikoshift.natsudroid.di.viewmodel.HiltResetPasswordViewModel
import io.mikoshift.natsudroid.feature.auth.AuthViewModelProviders
import io.mikoshift.natsudroid.feature.auth.authGraph
import io.mikoshift.natsudroid.feature.library.LibraryViewModelProviders
import io.mikoshift.natsudroid.feature.library.libraryGraph
import io.mikoshift.natsudroid.feature.profile.ProfileViewModelProviders
import io.mikoshift.natsudroid.feature.profile.profileGraph
import io.mikoshift.natsudroid.feature.reader.ReaderViewModelProviders
import io.mikoshift.natsudroid.feature.reader.readerGraph

fun NavGraphBuilder.natsudroidFeatureGraphs(navController: NavHostController) {
    authGraph(
        navController = navController,
        viewModels =
        AuthViewModelProviders(
            login = { hiltViewModel<HiltLoginViewModel>() },
            register = { hiltViewModel<HiltRegisterViewModel>() },
            forgotPassword = { hiltViewModel<HiltForgotPasswordViewModel>() },
            resetPassword = { hiltViewModel<HiltResetPasswordViewModel>() },
        ),
    )
    libraryGraph(
        navController = navController,
        viewModels =
        LibraryViewModelProviders(
            library = { hiltViewModel<HiltLibraryViewModel>() },
        ),
    )
    profileGraph(
        navController = navController,
        viewModels =
        ProfileViewModelProviders(
            profile = { hiltViewModel<HiltProfileViewModel>() },
            changePassword = { hiltViewModel<HiltChangePasswordViewModel>() },
        ),
    )
    readerGraph(
        navController = navController,
        viewModels =
        ReaderViewModelProviders(
            reader = { hiltViewModel<HiltReaderViewModel>() },
        ),
    )
}
