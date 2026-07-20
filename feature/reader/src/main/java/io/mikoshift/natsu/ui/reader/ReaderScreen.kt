package io.mikoshift.natsu.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsu.core.ui.CollectEffects
import io.mikoshift.natsu.feature.reader.R
import io.mikoshift.natsu.ui.reader.components.BlockContent
import io.mikoshift.natsu.ui.reader.components.DictionaryLookupSheet
import io.mikoshift.natsu.ui.reader.components.ReaderSettingsSheet
import io.mikoshift.natsu.ui.reader.components.TocSheet
import io.mikoshift.natsu.ui.theme.NatsuTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: ReaderViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ReaderEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
        }
    }

    ReaderScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::retry,
        onToggleToc = viewModel::toggleToc,
        onDismissToc = viewModel::dismissToc,
        onToggleSettings = viewModel::toggleSettings,
        onDismissSettings = viewModel::dismissSettings,
        onFontSizeChange = viewModel::onFontSizeChange,
        onLineSpacingChange = viewModel::onLineSpacingChange,
        onThemeChange = viewModel::onThemeChange,
        onFuriganaModeChange = viewModel::onFuriganaModeChange,
        onLookupWordAt = viewModel::lookupWordAt,
        onDismissLookup = viewModel::dismissLookup,
        onSectionSelected = { sectionId ->
            viewModel.scrollTargetForSection(sectionId)
        },
        onVisibleBlockChanged = viewModel::onVisibleBlockChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreenContent(
    uiState: ReaderUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleToc: () -> Unit,
    onDismissToc: () -> Unit,
    onToggleSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onFontSizeChange: (Double) -> Unit,
    onLineSpacingChange: (Double) -> Unit,
    onThemeChange: (io.mikoshift.natsu.core.model.ReaderTheme) -> Unit,
    onFuriganaModeChange: (io.mikoshift.natsu.core.model.FuriganaMode) -> Unit,
    onLookupWordAt: (String, String, Int) -> Unit,
    onDismissLookup: () -> Unit,
    onSectionSelected: (String) -> Int,
    onVisibleBlockChanged: (String, Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    var pendingScrollIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(uiState.contentState, uiState.initialScrollIndex) {
        if (uiState.contentState == ReaderContentState.Ready && uiState.initialScrollIndex > 0) {
            listState.scrollToItem(uiState.initialScrollIndex)
        }
    }

    LaunchedEffect(listState, uiState.contentState) {
        if (uiState.contentState != ReaderContentState.Ready) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val block = uiState.blocks.getOrNull(index) ?: return@collect
                onVisibleBlockChanged(block.sectionId, block.blockIndex, 0)
            }
    }

    if (uiState.lookupQuery != null) {
        DictionaryLookupSheet(
            query = uiState.lookupQuery,
            isLoading = uiState.lookupLoading,
            results = uiState.lookupResults,
            errorMessage = uiState.lookupErrorMessage,
            onDismiss = onDismissLookup,
        )
    }

    if (uiState.showSettings) {
        ReaderSettingsSheet(
            settings = uiState.readerSettings,
            onDismiss = onDismissSettings,
            onFontSizeChange = onFontSizeChange,
            onLineSpacingChange = onLineSpacingChange,
            onThemeChange = onThemeChange,
            onFuriganaModeChange = onFuriganaModeChange,
        )
    }

    if (uiState.showToc && uiState.toc.isNotEmpty()) {
        TocSheet(
            toc = uiState.toc,
            onDismiss = onDismissToc,
            onSectionSelected = { sectionId ->
                pendingScrollIndex = onSectionSelected(sectionId)
            },
        )
    }

    LaunchedEffect(pendingScrollIndex) {
        if (pendingScrollIndex >= 0) {
            listState.animateScrollToItem(pendingScrollIndex)
            pendingScrollIndex = -1
        }
    }

    val themeColors = uiState.readerSettings.theme.toColors()
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = uiState.readerSettings.fontSizeSp.sp,
        lineHeight = (uiState.readerSettings.fontSizeSp * uiState.readerSettings.lineSpacingMultiplier).sp,
        color = themeColors.onBackground,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back),
                        )
                    }
                },
                actions = {
                    if (uiState.contentState == ReaderContentState.Ready) {
                        IconButton(onClick = onToggleSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.reader_settings),
                            )
                        }
                    }
                    if (uiState.contentState == ReaderContentState.Ready && uiState.toc.isNotEmpty()) {
                        IconButton(onClick = onToggleToc) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = stringResource(R.string.reader_table_of_contents),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (uiState.contentState) {
            ReaderContentState.Loading,
            ReaderContentState.Downloading,
            -> LoadingState(
                message = when (uiState.contentState) {
                    ReaderContentState.Downloading -> stringResource(R.string.reader_downloading)
                    else -> stringResource(R.string.reader_loading)
                },
                modifier = Modifier.padding(padding),
            )
            ReaderContentState.Pending -> LoadingState(
                message = stringResource(R.string.reader_pending),
                modifier = Modifier.padding(padding),
            )
            ReaderContentState.Error -> ErrorState(
                message = uiState.errorMessage ?: stringResource(R.string.reader_error),
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
            ReaderContentState.Ready -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(
                    (4 * uiState.readerSettings.lineSpacingMultiplier).dp,
                ),
            ) {
                items(uiState.blocks, key = { it.id }) { block ->
                    BlockContent(
                        item = block,
                        bodyStyle = bodyStyle,
                        selectedWord = uiState.selectedWord,
                        onWordTap = onLookupWordAt,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.reader_retry))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReaderReadyPreview() {
    NatsuTheme {
        ReaderScreenContent(
            uiState = ReaderUiState(
                contentState = ReaderContentState.Ready,
                title = "Sample Book",
                blocks = listOf(
                    ReaderBlockItem(
                        id = "b0",
                        sectionId = "section-0",
                        blockIndex = 0,
                        block = io.mikoshift.natsu.core.model.content.ParagraphBlock(
                            id = "b0",
                            text = "Hello reader.",
                        ),
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onRetry = {},
            onToggleToc = {},
            onDismissToc = {},
            onToggleSettings = {},
            onDismissSettings = {},
            onFontSizeChange = {},
            onLineSpacingChange = {},
            onThemeChange = {},
            onFuriganaModeChange = {},
            onLookupWordAt = { _, _, _ -> },
            onDismissLookup = {},
            onSectionSelected = { 0 },
            onVisibleBlockChanged = { _, _, _ -> },
        )
    }
}
