package io.mikoshift.natsu.ui.library

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import io.mikoshift.natsu.core.domain.usecase.ImportDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.MarkDocumentDeletedUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveLibraryUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSyncStatusUseCase
import io.mikoshift.natsu.core.domain.usecase.SearchDocumentsUseCase
import io.mikoshift.natsu.core.domain.usecase.SyncDocumentsUseCase
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.testing.analytics.FakeAnalyticsTracker
import io.mikoshift.natsu.core.testing.fixture.AuthFixtures
import io.mikoshift.natsu.core.testing.fixture.DocumentFixtures
import io.mikoshift.natsu.core.testing.repository.FakeAuthRepository
import io.mikoshift.natsu.core.testing.repository.FakeDocumentRepository
import io.mikoshift.natsu.core.testing.repository.FakeSyncStatusRepository
import io.mikoshift.natsu.feature.library.R
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var documentRepository: FakeDocumentRepository
    private lateinit var syncStatusRepository: FakeSyncStatusRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_sync_failed) } returns "Sync failed"
        every { context.getString(R.string.error_search_failed) } returns "Search failed"
        every { context.getString(R.string.error_import_failed) } returns "Import failed"
        every { context.getString(R.string.error_delete_failed) } returns "Delete failed"
        every { context.getString(R.string.error_session_expired) } returns "Session expired"
        every { context.getString(R.string.error_network) } returns "Network error"
        every { context.getString(R.string.error_generic) } returns "Something went wrong"
        every { context.getString(R.string.import_uploading) } returns "Uploading…"
        every { context.getString(R.string.import_success, "New Book") } returns "\"New Book\" imported"
        every { context.getString(R.string.import_success, "Sample Document") } returns "\"Sample Document\" imported"

        documentRepository = FakeDocumentRepository(
            documents = listOf(DocumentFixtures.document(title = "Book One")),
        )
        syncStatusRepository = FakeSyncStatusRepository()
        authRepository = FakeAuthRepository(session = AuthFixtures.session())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_observesLibraryDocuments() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertEquals("Book One", viewModel.uiState.value.documents.single().title)
    }

    @Test
    fun init_observesSyncStatus_ignoresNonSyncingStates() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)

        syncStatusRepository.setFailed("Sync error")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun sync_onFailure_emitsErrorEffect() = runTest {
        documentRepository.syncResult = Result.failure(DocumentError.NetworkFailure)
        createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()
            assertEquals(
                LibraryEffect.ShowMessage("Network error"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sync_whenAlreadySyncing_doesNotStartAnotherSync() = runTest {
        createViewModel()
        advanceUntilIdle()
        val initialSyncCount = documentRepository.syncCallCount

        syncStatusRepository.setSyncing()
        advanceUntilIdle()
        viewModel.sync()
        advanceUntilIdle()

        assertEquals(initialSyncCount, documentRepository.syncCallCount)
    }

    @Test
    fun onSearchQueryChange_withEmptyQuery_clearsResults() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("query")
        advanceTimeBy(SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.searchResults)
        assertFalse(state.isSearching)
    }

    @Test
    fun onSearchQueryChange_afterDebounce_updatesSearchResults() = runTest {
        documentRepository.searchResult = Result.success(
            listOf(DocumentFixtures.searchResult(title = "Found Book")),
        )
        createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("found")
        advanceTimeBy(SEARCH_DEBOUNCE_MS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("found", state.searchQuery)
        assertFalse(state.isSearching)
        assertEquals("Found Book", state.searchResults?.single()?.title)
        assertEquals(listOf("found"), documentRepository.searchCalls)
    }

    @Test
    fun onSearchQueryChange_onFailure_emitsErrorEffect() = runTest {
        documentRepository.searchResult = Result.failure(DocumentError.NetworkFailure)
        createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onSearchQueryChange("query")
            advanceTimeBy(SEARCH_DEBOUNCE_MS)
            advanceUntilIdle()

            assertEquals(
                LibraryEffect.ShowMessage("Network error"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun importDocument_onSuccess_emitsSuccessEffect() = runTest {
        val imported = DocumentFixtures.document(id = "doc-2", title = "New Book")
        documentRepository.importResult = Result.success(imported)
        createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://books/new.epub"

        viewModel.effects.test {
            viewModel.importDocument(uri)
            advanceUntilIdle()

            assertEquals(
                LibraryEffect.ShowMessage("\"New Book\" imported"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isImporting)
        assertNull(viewModel.uiState.value.importProgressMessage)
        assertEquals(listOf("content://books/new.epub"), documentRepository.importCalls)
    }

    @Test
    fun importDocument_onFailure_emitsErrorEffect() = runTest {
        documentRepository.importResult = Result.failure(DocumentError.ImportFailed("Bad file"))
        createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://books/bad.epub"

        viewModel.effects.test {
            viewModel.importDocument(uri)
            advanceUntilIdle()

            assertEquals(
                LibraryEffect.ShowMessage("Bad file"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun importDocument_whenAlreadyImporting_isIgnored() = runTest {
        documentRepository.importResult = Result.success(DocumentFixtures.document())
        createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://books/a.epub"

        viewModel.importDocument(uri)
        viewModel.importDocument(uri)
        advanceUntilIdle()

        assertEquals(1, documentRepository.importCalls.size)
    }

    @Test
    fun deleteFlow_confirmDelete_onSuccess_triggersSync() = runTest {
        createViewModel()
        advanceUntilIdle()
        val syncCountAfterInit = documentRepository.syncCallCount

        viewModel.requestDelete("doc-1")
        assertEquals("doc-1", viewModel.uiState.value.deleteCandidateId)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteCandidateId)
        assertEquals(listOf("doc-1"), documentRepository.markDeletedCalls)
        assertEquals(syncCountAfterInit + 1, documentRepository.syncCallCount)
    }

    @Test
    fun deleteFlow_confirmDelete_onFailure_emitsErrorEffect() = runTest {
        documentRepository.markDeletedResult = Result.failure(DocumentError.NetworkFailure)
        createViewModel()
        advanceUntilIdle()

        viewModel.requestDelete("doc-1")

        viewModel.effects.test {
            viewModel.confirmDelete()
            advanceUntilIdle()

            assertEquals(
                LibraryEffect.ShowMessage("Network error"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteFlow_dismissDelete_clearsCandidate() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.requestDelete("doc-1")
        viewModel.dismissDelete()

        assertNull(viewModel.uiState.value.deleteCandidateId)
    }

    @Test
    fun confirmDelete_withoutCandidate_doesNothing() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(documentRepository.markDeletedCalls.isEmpty())
    }

    private fun createViewModel() {
        viewModel = LibraryViewModel(
            context = context,
            observeLibrary = ObserveLibraryUseCase(documentRepository),
            observeSyncStatus = ObserveSyncStatusUseCase(syncStatusRepository),
            syncDocuments = SyncDocumentsUseCase(
                authRepository,
                documentRepository,
                syncStatusRepository,
                FakeAnalyticsTracker(),
            ),
            importDocument = ImportDocumentUseCase(documentRepository, FakeAnalyticsTracker()),
            searchDocuments = SearchDocumentsUseCase(documentRepository),
            markDocumentDeleted = MarkDocumentDeletedUseCase(documentRepository),
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
