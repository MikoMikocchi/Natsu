package io.mikoshift.natsu.feature.reader

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.mikoshift.natsu.navigation.ReaderRoute
import io.mikoshift.natsu.ui.reader.ReaderScreen
import io.mikoshift.natsu.ui.reader.ReaderViewModel

fun NavGraphBuilder.readerGraph(navController: NavHostController) {
    composable<ReaderRoute> {
        val viewModel: ReaderViewModel = hiltViewModel()
        ReaderScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
