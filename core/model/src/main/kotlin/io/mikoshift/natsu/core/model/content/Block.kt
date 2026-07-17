package io.mikoshift.natsu.core.model.content

sealed interface Block {
    val id: String
}

data class ParagraphBlock(
    override val id: String,
    val text: String,
    val marks: List<Mark> = emptyList(),
) : Block

data class HeadingBlock(
    override val id: String,
    val level: Int,
    val text: String,
    val marks: List<Mark> = emptyList(),
) : Block

data class ImageBlock(
    override val id: String,
    val assetId: String,
    val alt: String? = null,
) : Block

data class BlockquoteBlock(
    override val id: String,
    val text: String,
    val marks: List<Mark> = emptyList(),
) : Block

data class ListItemBlock(
    override val id: String,
    val level: Int,
    val ordered: Boolean,
    val text: String,
    val marks: List<Mark> = emptyList(),
) : Block

data class DividerBlock(
    override val id: String,
) : Block

fun Block.plainText(): String = when (this) {
    is ParagraphBlock -> text
    is HeadingBlock -> text
    is BlockquoteBlock -> text
    is ListItemBlock -> text
    is ImageBlock -> alt.orEmpty()
    is DividerBlock -> ""
}
