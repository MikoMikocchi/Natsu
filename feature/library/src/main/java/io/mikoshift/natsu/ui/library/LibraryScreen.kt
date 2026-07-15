package io.mikoshift.natsu.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.feature.library.R
import io.mikoshift.natsu.ui.theme.NatsuTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.importStatusMessage) {
        uiState.importStatusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearImportStatus()
        }
    }

    LibraryScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToProfile = onNavigateToProfile,
        onImportClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSync = viewModel::sync,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreenContent(
    uiState: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToProfile: () -> Unit,
    onImportClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSync: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    if (uiState.deleteCandidateId != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.delete_document_title)) },
            text = { Text(stringResource(R.string.delete_document_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isImporting) {
                FloatingActionButton(onClick = onImportClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.import_action))
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = onSync,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_library)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                )

                if (uiState.isImporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.importStatusMessage ?: stringResource(R.string.importing),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val searchResults = uiState.searchResults
                    if (searchResults != null) {
                        if (searchResults.isEmpty()) {
                            EmptyState(message = stringResource(R.string.no_matches_found))
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(searchResults, key = { it.id }) { result ->
                                    SearchResultCard(result = result)
                                }
                            }
                        }
                    } else if (uiState.documents.isEmpty() && !uiState.isSyncing) {
                        EmptyState(message = stringResource(R.string.library_empty))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uiState.documents, key = { it.id }) { document ->
                                DocumentCard(
                                    document = document,
                                    onDelete = { onRequestDelete(document.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DocumentCard(
    document: DocumentListItem,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = document.title.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleMedium,
            )
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(document.status.label()) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text(document.sourceFormat.label()) },
                )
            }
            if (document.status == DocumentStatus.FAILED && document.importError != null) {
                Text(
                    text = document.importError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (document.status == DocumentStatus.PENDING) {
                Text(
                    text = stringResource(R.string.import_in_progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResultItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = result.title, style = MaterialTheme.typography.titleMedium)
            result.snippets.take(2).forEach { snippet ->
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentStatus.label(): String = when (this) {
    DocumentStatus.PENDING -> stringResource(R.string.status_pending)
    DocumentStatus.READY -> stringResource(R.string.status_ready)
    DocumentStatus.FAILED -> stringResource(R.string.status_failed)
}

@Composable
private fun SourceFormat.label(): String = when (this) {
    SourceFormat.EPUB -> stringResource(R.string.format_epub)
    SourceFormat.MARKDOWN -> stringResource(R.string.format_markdown)
    SourceFormat.PLAIN_TEXT -> stringResource(R.string.format_text)
}

private val IMPORT_MIME_TYPES = arrayOf(
    "application/epub+zip",
    "text/plain",
    "text/markdown",
    "application/octet-stream",
)

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    NatsuTheme {
        LibraryScreenContent(
            uiState = LibraryUiState(
                documents = listOf(
                    DocumentListItem(
                        id = "1",
                        title = "The Great Gatsby",
                        status = DocumentStatus.READY,
                        sourceFormat = SourceFormat.EPUB,
                    ),
                    DocumentListItem(
                        id = "2",
                        title = "Notes",
                        status = DocumentStatus.PENDING,
                        sourceFormat = SourceFormat.MARKDOWN,
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateToProfile = {},
            onImportClick = {},
            onSearchQueryChange = {},
            onSync = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenEmptyPreview() {
    NatsuTheme {
        LibraryScreenContent(
            uiState = LibraryUiState(),
            snackbarHostState = SnackbarHostState(),
            onNavigateToProfile = {},
            onImportClick = {},
            onSearchQueryChange = {},
            onSync = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDelete = {},
        )
    }
}
