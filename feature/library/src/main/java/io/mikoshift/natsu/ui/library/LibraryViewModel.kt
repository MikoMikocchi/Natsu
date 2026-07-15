package io.mikoshift.natsu.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.core.domain.usecase.ImportDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveLibraryUseCase
import io.mikoshift.natsu.core.domain.usecase.SyncDocumentsUseCase
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.SyncState
import io.mikoshift.natsu.feature.library.R
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val observeLibrary: ObserveLibraryUseCase,
    private val syncDocuments: SyncDocumentsUseCase,
    private val importDocument: ImportDocumentUseCase,
    syncStatusRepository: SyncStatusRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeLibrary().collect { documents ->
                _uiState.update { state ->
                    state.copy(documents = documents.map { it.toListItem() })
                }
            }
        }
        viewModelScope.launch {
            syncStatusRepository.syncState.collect { state ->
                _uiState.update { it.copy(isSyncing = state is SyncState.Syncing) }
            }
        }
        sync()
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            syncDocuments().fold(
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            error = (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_sync_failed),
                        )
                    }
                },
                onSuccess = {},
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, error = null) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }
            documentRepository.search(trimmed).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = results.map { result -> result.toSearchResultItem() },
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_search_failed),
                        )
                    }
                },
            )
        }
    }

    fun importDocument(uri: Uri) {
        if (_uiState.value.isImporting) return
        _uiState.update {
            it.copy(
                isImporting = true,
                importStatusMessage = context.getString(R.string.import_uploading),
                error = null,
            )
        }
        viewModelScope.launch {
            importDocument(uri.toString()).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importStatusMessage = context.getString(R.string.import_success, document.title),
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importStatusMessage = null,
                            error = (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_import_failed),
                        )
                    }
                },
            )
        }
    }

    fun requestDelete(id: String) {
        _uiState.update { it.copy(deleteCandidateId = id) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteCandidateId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteCandidateId ?: return
        _uiState.update { it.copy(deleteCandidateId = null) }
        viewModelScope.launch {
            documentRepository.markDeleted(id).fold(
                onSuccess = { sync() },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            error = (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_delete_failed),
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearImportStatus() {
        _uiState.update { it.copy(importStatusMessage = null) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
