package io.mikoshift.natsu.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.usecase.ImportDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.MarkDocumentDeletedUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveLibraryUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSyncStatusUseCase
import io.mikoshift.natsu.core.domain.usecase.SearchDocumentsUseCase
import io.mikoshift.natsu.core.domain.usecase.SyncDocumentsUseCase
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SyncState
import io.mikoshift.natsu.feature.library.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val observeLibrary: ObserveLibraryUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val syncDocuments: SyncDocumentsUseCase,
    private val importDocument: ImportDocumentUseCase,
    private val searchDocuments: SearchDocumentsUseCase,
    private val markDocumentDeleted: MarkDocumentDeletedUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
            observeSyncStatus().collect { state ->
                _uiState.update { it.copy(isSyncing = state is SyncState.Syncing) }
            }
        }
        sync()
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            syncDocuments().fold(
                onFailure = { throwable ->
                    sendEffect(
                        LibraryEffect.ShowMessage(
                            (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_sync_failed),
                        ),
                    )
                },
                onSuccess = {},
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                _uiState.update { it.copy(isSearching = true) }
                searchDocuments(trimmed).fold(
                    onSuccess = { results ->
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                searchResults = results.map { result -> result.toSearchResultItem() },
                            )
                        }
                    },
                    onFailure = { throwable ->
                        _uiState.update { it.copy(isSearching = false) }
                        sendEffect(
                            LibraryEffect.ShowMessage(
                                (throwable as? DocumentError)?.toUserMessage()
                                    ?: context.getString(R.string.error_search_failed),
                            ),
                        )
                    },
                )
            }
    }

    fun importDocument(uri: Uri) {
        if (_uiState.value.isImporting) return
        _uiState.update {
            it.copy(
                isImporting = true,
                importProgressMessage = context.getString(R.string.import_uploading),
            )
        }
        viewModelScope.launch {
            importDocument(uri.toString()).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(isImporting = false, importProgressMessage = null)
                    }
                    sendEffect(
                        LibraryEffect.ShowMessage(
                            context.getString(R.string.import_success, document.title),
                        ),
                    )
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isImporting = false, importProgressMessage = null)
                    }
                    sendEffect(
                        LibraryEffect.ShowMessage(
                            (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_import_failed),
                        ),
                    )
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

    fun openDocument(document: DocumentListItem) {
        when (document.status) {
            DocumentStatus.READY ->
                sendEffect(
                    LibraryEffect.NavigateToReader(documentId = document.id),
                )
            DocumentStatus.PENDING ->
                sendEffect(
                    LibraryEffect.ShowMessage(context.getString(R.string.document_not_ready)),
                )
            DocumentStatus.FAILED ->
                sendEffect(
                    LibraryEffect.ShowMessage(
                        document.importError ?: context.getString(R.string.document_import_failed_open),
                    ),
                )
        }
    }

    fun openSearchResult(result: SearchResultItem) {
        sendEffect(
            LibraryEffect.NavigateToReader(
                documentId = result.id,
                initialCharOffset = result.initialCharOffset,
            ),
        )
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteCandidateId ?: return
        _uiState.update { it.copy(deleteCandidateId = null) }
        viewModelScope.launch {
            markDocumentDeleted(id).fold(
                onSuccess = { sync() },
                onFailure = { throwable ->
                    sendEffect(
                        LibraryEffect.ShowMessage(
                            (throwable as? DocumentError)?.toUserMessage()
                                ?: context.getString(R.string.error_delete_failed),
                        ),
                    )
                },
            )
        }
    }

    private fun sendEffect(effect: LibraryEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun DocumentError.toUserMessage(): String = when (this) {
        is DocumentError.ValidationError -> fieldErrors.values.flatten().joinToString(", ")
        DocumentError.Unauthorized -> context.getString(R.string.error_session_expired)
        DocumentError.NetworkFailure -> context.getString(R.string.error_network)
        DocumentError.PackageNotReady -> context.getString(R.string.document_not_ready)
        is DocumentError.ImportFailed -> reason ?: context.getString(R.string.error_import_failed)
        is DocumentError.Unknown -> errorMessage ?: context.getString(R.string.error_generic)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
