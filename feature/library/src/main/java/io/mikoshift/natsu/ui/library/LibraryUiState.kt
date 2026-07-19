package io.mikoshift.natsu.ui.library

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentSearchResult
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat

data class LibraryUiState(
    val documents: List<DocumentListItem> = emptyList(),
    val isSyncing: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem>? = null,
    val isImporting: Boolean = false,
    val importProgressMessage: String? = null,
    val deleteCandidateId: String? = null,
)

sealed interface LibraryEffect {
    data class ShowMessage(val text: String) : LibraryEffect

    data class NavigateToReader(val documentId: String, val initialCharOffset: Int? = null) : LibraryEffect
}

data class DocumentListItem(
    val id: String,
    val title: String,
    val status: DocumentStatus,
    val sourceFormat: SourceFormat,
    val importError: String? = null,
    val hasPackage: Boolean = false,
)

data class SearchResultItem(
    val id: String,
    val title: String,
    val snippets: List<String>,
    val initialCharOffset: Int? = null,
)

fun Document.toListItem(): DocumentListItem = DocumentListItem(
    id = id,
    title = title,
    status = status,
    sourceFormat = sourceFormat,
    importError = importError,
    hasPackage = localPackagePath != null,
)

fun DocumentSearchResult.toSearchResultItem(): SearchResultItem = SearchResultItem(
    id = id,
    title = title,
    snippets = matches.map { it.snippet },
    initialCharOffset = matches.firstOrNull()?.charOffset,
)
