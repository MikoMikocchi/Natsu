package io.mikoshift.natsu.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.domain.usecase.EnsurePackageDownloadedUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.OpenDocumentPackageUseCase
import io.mikoshift.natsu.core.domain.usecase.UpdateReadingProgressUseCase
import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentMetadata
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.core.model.content.DocumentPackage
import io.mikoshift.natsu.core.model.content.ManifestSection
import io.mikoshift.natsu.core.model.content.PackageManifest
import io.mikoshift.natsu.core.model.content.ParagraphBlock
import io.mikoshift.natsu.core.model.content.ReadingPosition
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
    private lateinit var documentPackageRepository: DocumentPackageRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        observeDocument = mockk()
        ensurePackageDownloaded = mockk()
        openDocumentPackage = mockk()
        updateReadingProgress = mockk()
        documentPackageRepository = mockk(relaxed = true)
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
        val savedStateHandle = SavedStateHandle(
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
            documentPackageRepository = documentPackageRepository,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun sampleDocument(): Document = Document(
        metadata = DocumentMetadata(
            id = "doc-1",
            title = "My Book",
            sourceFormat = SourceFormat.EPUB,
            status = DocumentStatus.READY,
        ),
    )

    private fun samplePackage(): DocumentPackage = DocumentPackage(
        manifest = PackageManifest(
            schemaVersion = 2,
            title = "My Book",
            authors = emptyList(),
            language = null,
            coverAssetId = null,
            sourceFormat = SourceFormat.EPUB,
            toc = emptyList(),
            sections = listOf(
                ManifestSection(
                    id = "section-0",
                    title = "Chapter",
                    path = "sections/section-0.json",
                    wordCount = 1,
                    checksum = "abc",
                ),
            ),
        ),
        sections = mapOf(
            "section-0" to listOf(
                ParagraphBlock(id = "b0", text = "Hello"),
            ),
        ),
    )
}
