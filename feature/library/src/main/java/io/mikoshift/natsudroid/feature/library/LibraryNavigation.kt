package io.mikoshift.natsudroid.feature.library

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsudroid.navigation.HomeRoute
import io.mikoshift.natsudroid.navigation.ProfileRoute
import io.mikoshift.natsudroid.navigation.ReaderRoute
import io.mikoshift.natsudroid.ui.library.LibraryScreen
import io.mikoshift.natsudroid.ui.library.LibraryViewModel

class LibraryViewModelProviders(val library: @Composable () -> LibraryViewModel)

fun NavGraphBuilder.libraryGraph(navController: NavHostController, viewModels: LibraryViewModelProviders) {
    composable<HomeRoute> {
        LibraryScreen(
            viewModel = viewModels.library(),
            onNavigateToProfile = { navController.navigate(ProfileRoute) },
            onNavigateToReader = { documentId, initialCharOffset ->
                navController.navigate(
                    ReaderRoute(
                        documentId = documentId,
                        initialCharOffset = initialCharOffset,
                    ),
                )
            },
        )
    }
}
