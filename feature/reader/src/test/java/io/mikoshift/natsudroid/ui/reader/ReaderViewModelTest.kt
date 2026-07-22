package io.mikoshift.natsudroid.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mikoshift.natsudroid.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsudroid.core.domain.usecase.EnsurePackageDownloadedUseCase
import io.mikoshift.natsudroid.core.domain.usecase.ListDictionariesUseCase
import io.mikoshift.natsudroid.core.domain.usecase.LookupWordUseCase
import io.mikoshift.natsudroid.core.domain.usecase.ObserveDocumentUseCase
import io.mikoshift.natsudroid.core.domain.usecase.ObserveReaderSettingsUseCase
import io.mikoshift.natsudroid.core.domain.usecase.OpenDocumentPackageUseCase
import io.mikoshift.natsudroid.core.domain.usecase.UpdateReaderSettingsUseCase
import io.mikoshift.natsudroid.core.domain.usecase.UpdateReadingProgressUseCase
import io.mikoshift.natsudroid.core.model.Document
import io.mikoshift.natsudroid.core.model.DocumentMetadata
import io.mikoshift.natsudroid.core.model.DocumentStatus
import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import io.mikoshift.natsudroid.core.model.SourceFormat
import io.mikoshift.natsudroid.core.model.content.DocumentPackage
import io.mikoshift.natsudroid.core.model.content.ManifestSection
import io.mikoshift.natsudroid.core.model.content.PackageManifest
import io.mikoshift.natsudroid.core.model.content.ParagraphBlock
import io.mikoshift.natsudroid.core.model.content.ReadingPosition
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReaderViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var observeDocument: ObserveDocumentUseCase
    private lateinit var ensurePackageDownloaded: EnsurePackageDownloadedUseCase
    private lateinit var openDocumentPackage: OpenDocumentPackageUseCase
    private lateinit var updateReadingProgress: UpdateReadingProgressUseCase
    private lateinit var observeReaderSettings: ObserveReaderSettingsUseCase
    private lateinit var updateReaderSettings: UpdateReaderSettingsUseCase
    private lateinit var lookupWord: LookupWordUseCase
    private lateinit var listDictionaries: ListDictionariesUseCase
    private lateinit var documentPackageRepository: DocumentPackageRepository

    private val defaultReaderSettings =
        ReaderSettings(
            fontSizeSp = 16.0,
            lineSpacingMultiplier = 1.5,
            theme = ReaderTheme.LIGHT,
            furiganaMode = FuriganaMode.OFF,
            updatedAtMs = 0L,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        observeDocument = mockk()
        ensurePackageDownloaded = mockk()
        openDocumentPackage = mockk()
        updateReadingProgress = mockk()
        observeReaderSettings = mockk()
        updateReaderSettings = mockk()
        lookupWord = mockk()
        listDictionaries = mockk()
        documentPackageRepository = mockk(relaxed = true)
        every { observeReaderSettings() } returns flowOf(defaultReaderSettings)
        coEvery { observeReaderSettings.refresh() } returns Result.success(defaultReaderSettings)
        coEvery { listDictionaries() } returns Result.success(
            io.mikoshift.natsudroid.core.model.DictionaryPage(
                dictionaries = emptyList(),
                pagination = io.mikoshift.natsudroid.core.model.DictionaryPagination(1, 50, 0, 0),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDocument_readyDocument_showsContent() = runTest(testDispatcher) {
        val document = sampleDocument()
        val documentPackage = samplePackage()
        every { observeDocument("doc-1") } returns flowOf(document)
        coEvery { ensurePackageDownloaded("doc-1") } returns Result.success(Unit)
        coEvery { openDocumentPackage("doc-1") } returns Result.success(documentPackage)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(ReaderContentState.Ready, viewModel.uiState.value.contentState)
        assertEquals("My Book", viewModel.uiState.value.title)
        assertEquals(1, viewModel.uiState.value.blocks.size)
    }

    @Test
    fun onVisibleBlockChanged_debouncedProgressUpdate() = runTest(testDispatcher) {
        val document = sampleDocument()
        val documentPackage = samplePackage()
        every { observeDocument("doc-1") } returns flowOf(document)
        coEvery { ensurePackageDownloaded("doc-1") } returns Result.success(Unit)
        coEvery { openDocumentPackage("doc-1") } returns Result.success(documentPackage)
        coEvery { updateReadingProgress("doc-1", any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVisibleBlockChanged("section-0", 0, 0)
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            updateReadingProgress(
                "doc-1",
                ReadingPosition(
                    sectionId = "section-0",
                    blockIndex = 0,
                    blockCharOffset = 0,
                    globalCharOffset = 0,
                ),
            )
        }
    }

    private fun createViewModel(): ReaderViewModel {
        val savedStateHandle =
            SavedStateHandle(
                mapOf(
                    "documentId" to "doc-1",
                    "initialCharOffset" to null,
                ),
            )
        return ReaderViewModel(
            context = context,
            observeDocument = observeDocument,
            ensurePackageDownloaded = ensurePackageDownloaded,
            openDocumentPackage = openDocumentPackage,
            updateReadingProgress = updateReadingProgress,
            observeReaderSettings = observeReaderSettings,
            updateReaderSettings = updateReaderSettings,
            lookupWord = lookupWord,
            listDictionaries = listDictionaries,
            documentPackageRepository = documentPackageRepository,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun sampleDocument(): Document = Document(
        metadata =
        DocumentMetadata(
            id = "doc-1",
            title = "My Book",
            sourceFormat = SourceFormat.EPUB,
            status = DocumentStatus.READY,
        ),
    )

    private fun samplePackage(): DocumentPackage = DocumentPackage(
        manifest =
        PackageManifest(
            schemaVersion = 2,
            title = "My Book",
            authors = emptyList(),
            language = null,
            coverAssetId = null,
            sourceFormat = SourceFormat.EPUB,
            toc = emptyList(),
            sections =
            listOf(
                ManifestSection(
                    id = "section-0",
                    title = "Chapter",
                    path = "sections/section-0.json",
                    wordCount = 1,
                    checksum = "abc",
                ),
            ),
        ),
        sections =
        mapOf(
            "section-0" to
                listOf(
                    ParagraphBlock(id = "b0", text = "Hello"),
                ),
        ),
    )
}
