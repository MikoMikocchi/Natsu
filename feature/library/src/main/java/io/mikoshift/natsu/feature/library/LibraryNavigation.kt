package io.mikoshift.natsu.feature.library

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsu.navigation.HomeRoute
import io.mikoshift.natsu.navigation.ProfileRoute
import io.mikoshift.natsu.navigation.ReaderRoute
import io.mikoshift.natsu.ui.library.LibraryScreen
import io.mikoshift.natsu.ui.library.LibraryViewModel

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
