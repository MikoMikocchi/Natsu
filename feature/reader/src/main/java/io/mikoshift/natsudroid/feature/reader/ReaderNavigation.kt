package io.mikoshift.natsudroid.feature.reader

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsudroid.navigation.ReaderRoute
import io.mikoshift.natsudroid.ui.reader.ReaderScreen
import io.mikoshift.natsudroid.ui.reader.ReaderViewModel

class ReaderViewModelProviders(val reader: @Composable () -> ReaderViewModel)

fun NavGraphBuilder.readerGraph(navController: NavHostController, viewModels: ReaderViewModelProviders) {
    composable<ReaderRoute> {
        ReaderScreen(
            viewModel = viewModels.reader(),
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
