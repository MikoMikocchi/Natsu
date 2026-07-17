package io.mikoshift.natsu.ui.reader

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
    val errorMessage: String? = null,
)

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
