package io.mikoshift.natsu.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.domain.usecase.EnsurePackageDownloadedUseCase
import io.mikoshift.natsu.core.domain.usecase.LookupWordUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveReaderSettingsUseCase
import io.mikoshift.natsu.core.domain.usecase.OpenDocumentPackageUseCase
import io.mikoshift.natsu.core.domain.usecase.UpdateReaderSettingsUseCase
import io.mikoshift.natsu.core.domain.usecase.UpdateReadingProgressUseCase
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.FuriganaMode
import io.mikoshift.natsu.core.model.ReaderTheme
import io.mikoshift.natsu.core.model.content.ImageBlock
import io.mikoshift.natsu.core.model.content.PlainTextIndex
import io.mikoshift.natsu.core.model.content.ReadingPosition
import io.mikoshift.natsu.feature.reader.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val observeDocument: ObserveDocumentUseCase,
    private val ensurePackageDownloaded: EnsurePackageDownloadedUseCase,
    private val openDocumentPackage: OpenDocumentPackageUseCase,
    private val updateReadingProgress: UpdateReadingProgressUseCase,
    private val observeReaderSettings: ObserveReaderSettingsUseCase,
    private val updateReaderSettings: UpdateReaderSettingsUseCase,
    private val lookupWord: LookupWordUseCase,
    private val documentPackageRepository: DocumentPackageRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val documentId: String = savedStateHandle.get<String>("documentId").orEmpty()
    private val initialCharOffset: Int? = savedStateHandle.get<Int>("initialCharOffset")

    private val _uiState = MutableStateFlow(ReaderUiState(documentId = documentId))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ReaderEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var plainTextIndex: PlainTextIndex? = null
    private var lastSavedPosition: ReadingPosition? = null

    init {
        if (documentId.isBlank()) {
            _uiState.update {
                it.copy(
                    contentState = ReaderContentState.Error,
                    errorMessage = context.getString(R.string.reader_not_found),
                )
            }
        } else {
            observeSettings()
            loadDocument()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeReaderSettings().collect { settings ->
                settings?.let { value ->
                    _uiState.update { it.copy(readerSettings = value) }
                }
            }
        }
        viewModelScope.launch {
            observeReaderSettings.refresh()
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun onFontSizeChange(value: Double) {
        updateSetting(fontSizeSp = value)
    }

    fun onLineSpacingChange(value: Double) {
        updateSetting(lineSpacingMultiplier = value)
    }

    fun onThemeChange(theme: ReaderTheme) {
        updateSetting(theme = theme)
    }

    fun onFuriganaModeChange(mode: FuriganaMode) {
        updateSetting(furiganaMode = mode)
    }

    fun lookupWordAt(blockId: String, text: String, charOffset: Int) {
        val range = extractWordAtOffset(text, charOffset) ?: return
        val query = extractLookupQuery(text.substring(range)) ?: return
        _uiState.update {
            it.copy(
                selectedWord = SelectedWord(blockId = blockId, range = range),
                lookupQuery = query,
                lookupLoading = true,
                lookupResults = emptyList(),
                lookupErrorMessage = null,
            )
        }
        viewModelScope.launch {
            lookupWord(query).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            lookupLoading = false,
                            lookupResults = results,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            lookupLoading = false,
                            lookupErrorMessage = throwable.message ?: context.getString(R.string.error_generic),
                        )
                    }
                },
            )
        }
    }

    fun dismissLookup() {
        _uiState.update {
            it.copy(
                lookupQuery = null,
                lookupLoading = false,
                lookupResults = emptyList(),
                lookupErrorMessage = null,
                selectedWord = null,
            )
        }
    }

    private fun updateSetting(
        fontSizeSp: Double? = null,
        lineSpacingMultiplier: Double? = null,
        theme: ReaderTheme? = null,
        furiganaMode: FuriganaMode? = null,
    ) {
        val current = _uiState.value.readerSettings
        _uiState.update {
            it.copy(
                readerSettings =
                current.copy(
                    fontSizeSp = fontSizeSp ?: current.fontSizeSp,
                    lineSpacingMultiplier = lineSpacingMultiplier ?: current.lineSpacingMultiplier,
                    theme = theme ?: current.theme,
                    furiganaMode = furiganaMode ?: current.furiganaMode,
                ),
            )
        }
        viewModelScope.launch {
            updateReaderSettings(
                fontSizeSp = fontSizeSp,
                lineSpacingMultiplier = lineSpacingMultiplier,
                theme = theme,
                furiganaMode = furiganaMode,
            )
        }
    }

    fun retry() {
        loadDocument()
    }

    fun toggleToc() {
        _uiState.update { it.copy(showToc = !it.showToc) }
    }

    fun dismissToc() {
        _uiState.update { it.copy(showToc = false) }
    }

    fun onVisibleBlockChanged(sectionId: String, blockIndex: Int, blockCharOffset: Int) {
        val index = plainTextIndex ?: return
        val position =
            index.locateFromProgress(
                sectionId = sectionId,
                blockIndex = blockIndex,
                blockCharOffset = blockCharOffset,
                globalCharOffset = 0,
            ) ?: return
        if (position == lastSavedPosition) return

        progressJob?.cancel()
        progressJob =
            viewModelScope.launch {
                delay(PROGRESS_DEBOUNCE_MS)
                lastSavedPosition = position
                updateReadingProgress(documentId, position)
            }
    }

    fun scrollTargetForSection(sectionId: String): Int {
        val blocks = _uiState.value.blocks
        return blocks.indexOfFirst { it.sectionId == sectionId }.coerceAtLeast(0)
    }

    override fun onCleared() {
        lastSavedPosition?.let { position ->
            viewModelScope.launch {
                updateReadingProgress(documentId, position)
            }
        }
        super.onCleared()
    }

    private fun loadDocument() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        contentState = ReaderContentState.Loading,
                        errorMessage = null,
                    )
                }

                val document = observeDocument(documentId).filterNotNull().first()

                when (document.status) {
                    DocumentStatus.PENDING -> {
                        _uiState.update {
                            it.copy(
                                contentState = ReaderContentState.Pending,
                                title = document.title,
                            )
                        }
                        val readyDocument =
                            observeDocument(documentId)
                                .filterNotNull()
                                .first { doc -> doc.status != DocumentStatus.PENDING }
                        when (readyDocument.status) {
                            DocumentStatus.FAILED -> {
                                _uiState.update {
                                    it.copy(
                                        contentState = ReaderContentState.Error,
                                        title = readyDocument.title,
                                        errorMessage =
                                        readyDocument.importError
                                            ?: context.getString(R.string.reader_error),
                                    )
                                }
                                return@launch
                            }
                            DocumentStatus.READY -> openReadyDocument(readyDocument)
                            DocumentStatus.PENDING -> Unit
                        }
                        return@launch
                    }
                    DocumentStatus.FAILED -> {
                        _uiState.update {
                            it.copy(
                                contentState = ReaderContentState.Error,
                                title = document.title,
                                errorMessage =
                                document.importError
                                    ?: context.getString(R.string.reader_error),
                            )
                        }
                        return@launch
                    }
                    DocumentStatus.READY -> openReadyDocument(document)
                }
            }
    }

    private suspend fun openReadyDocument(document: io.mikoshift.natsu.core.model.Document) {
        _uiState.update {
            it.copy(
                contentState = ReaderContentState.Downloading,
                title = document.title,
            )
        }
        ensurePackageDownloaded(documentId).getOrElse { error ->
            _uiState.update {
                it.copy(
                    contentState = ReaderContentState.Error,
                    errorMessage = error.toUserMessage(),
                )
            }
            return
        }

        val documentPackage =
            openDocumentPackage(documentId).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        contentState = ReaderContentState.Error,
                        errorMessage = error.toUserMessage(),
                    )
                }
                return
            }

        val index = PlainTextIndex.fromDocumentPackage(documentPackage)
        plainTextIndex = index

        val readerBlocks =
            documentPackage.blocksInReadingOrder().map { readerBlock ->
                val assetPath =
                    (readerBlock.block as? ImageBlock)?.let { image ->
                        documentPackageRepository.resolveAssetPath(documentId, image.assetId)
                    }
                ReaderBlockItem(
                    id = readerBlock.block.id,
                    sectionId = readerBlock.sectionId,
                    blockIndex = readerBlock.blockIndex,
                    block = readerBlock.block,
                    assetPath = assetPath,
                )
            }

        val initialPosition = resolveInitialPosition(document, index, readerBlocks)

        _uiState.update {
            it.copy(
                contentState = ReaderContentState.Ready,
                title = documentPackage.manifest.title.ifBlank { document.title },
                blocks = readerBlocks,
                toc = documentPackage.manifest.toc,
                initialScrollIndex = initialPosition,
            )
        }
    }

    private fun resolveInitialPosition(
        document: io.mikoshift.natsu.core.model.Document,
        index: PlainTextIndex,
        blocks: List<ReaderBlockItem>,
    ): Int {
        val position =
            when {
                initialCharOffset != null -> index.locateFromSearch(document.title, initialCharOffset)
                document.lastReadSectionId != null ->
                    index.locateFromProgress(
                        sectionId = document.lastReadSectionId,
                        blockIndex = document.lastReadBlockIndex,
                        blockCharOffset = document.lastReadBlockCharOffset,
                        globalCharOffset = document.lastReadCharOffset,
                    )
                document.lastReadCharOffset > 0 -> index.locateGlobalOffset(document.lastReadCharOffset)
                else -> null
            } ?: return 0

        return index.readerBlockIndex(
            blocks =
            blocks.map {
                io.mikoshift.natsu.core.model.content.ReaderBlock(
                    sectionId = it.sectionId,
                    blockIndex = it.blockIndex,
                    block = it.block,
                )
            },
            position = position,
        )
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        DocumentError.Unauthorized -> context.getString(R.string.error_session_expired)
        DocumentError.NetworkFailure -> context.getString(R.string.error_network)
        DocumentError.PackageNotReady -> context.getString(R.string.reader_error)
        is DocumentError -> message ?: context.getString(R.string.error_generic)
        else -> message ?: context.getString(R.string.error_generic)
    }

    private companion object {
        const val PROGRESS_DEBOUNCE_MS = 500L
    }
}
