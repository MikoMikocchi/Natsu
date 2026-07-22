package io.mikoshift.natsu.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import io.mikoshift.natsu.di.viewmodel.HiltChangePasswordViewModel
import io.mikoshift.natsu.di.viewmodel.HiltForgotPasswordViewModel
import io.mikoshift.natsu.di.viewmodel.HiltLibraryViewModel
import io.mikoshift.natsu.di.viewmodel.HiltLoginViewModel
import io.mikoshift.natsu.di.viewmodel.HiltProfileViewModel
import io.mikoshift.natsu.di.viewmodel.HiltReaderViewModel
import io.mikoshift.natsu.di.viewmodel.HiltRegisterViewModel
import io.mikoshift.natsu.di.viewmodel.HiltResetPasswordViewModel
import io.mikoshift.natsu.feature.auth.AuthViewModelProviders
import io.mikoshift.natsu.feature.auth.authGraph
import io.mikoshift.natsu.feature.library.LibraryViewModelProviders
import io.mikoshift.natsu.feature.library.libraryGraph
import io.mikoshift.natsu.feature.profile.ProfileViewModelProviders
import io.mikoshift.natsu.feature.profile.profileGraph
import io.mikoshift.natsu.feature.reader.ReaderViewModelProviders
import io.mikoshift.natsu.feature.reader.readerGraph

fun NavGraphBuilder.natsuFeatureGraphs(navController: NavHostController) {
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
