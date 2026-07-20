package io.mikoshift.natsu.ui.reader

import io.mikoshift.natsu.core.model.FuriganaMode
import io.mikoshift.natsu.core.model.ReaderSettings
import io.mikoshift.natsu.core.model.ReaderTheme
import io.mikoshift.natsu.core.model.content.Block
import io.mikoshift.natsu.core.model.content.TocNode

enum class ReaderContentState {
    Loading,
    Pending,
    Downloading,
    Ready,
    Error,
}

data class ReaderUiState(
    val contentState: ReaderContentState = ReaderContentState.Loading,
    val documentId: String = "",
    val title: String = "",
    val blocks: List<ReaderBlockItem> = emptyList(),
    val toc: List<TocNode> = emptyList(),
    val initialScrollIndex: Int = 0,
    val showToc: Boolean = false,
    val showSettings: Boolean = false,
    val readerSettings: ReaderSettings =
        ReaderSettings(
            fontSizeSp = 16.0,
            lineSpacingMultiplier = 1.8,
            theme = ReaderTheme.LIGHT,
            furiganaMode = FuriganaMode.OFF,
            updatedAtMs = 0L,
        ),
    val lookupQuery: String? = null,
    val lookupLoading: Boolean = false,
    val lookupResults: List<io.mikoshift.natsu.core.model.DictionaryLookupResult> = emptyList(),
    val lookupErrorMessage: String? = null,
    val lookupSuggestEnableDictionary: Boolean = false,
    val selectedWord: SelectedWord? = null,
    val errorMessage: String? = null,
)

data class SelectedWord(val blockId: String, val range: IntRange)

data class ReaderBlockItem(
    val id: String,
    val sectionId: String,
    val blockIndex: Int,
    val block: Block,
    val assetPath: String? = null,
)

sealed interface ReaderEffect {
    data class ShowMessage(val text: String) : ReaderEffect
}
