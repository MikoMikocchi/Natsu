package io.mikoshift.natsu.feature.reader

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsu.navigation.ReaderRoute
import io.mikoshift.natsu.ui.reader.ReaderScreen
import io.mikoshift.natsu.ui.reader.ReaderViewModel

class ReaderViewModelProviders(val reader: @Composable () -> ReaderViewModel)

fun NavGraphBuilder.readerGraph(navController: NavHostController, viewModels: ReaderViewModelProviders) {
    composable<ReaderRoute> {
        ReaderScreen(
            viewModel = viewModels.reader(),
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
