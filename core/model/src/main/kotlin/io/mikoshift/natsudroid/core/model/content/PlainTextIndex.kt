package io.mikoshift.natsudroid.core.model.content

data class BlockAnchor(val sectionId: String, val blockIndex: Int, val globalStartOffset: Int, val textLength: Int)

class PlainTextIndex private constructor(val plainText: String, private val anchors: List<BlockAnchor>) {
    val length: Int get() = plainText.length

    fun locateGlobalOffset(offset: Int): ReadingPosition? {
        if (anchors.isEmpty()) return null
        val clamped = offset.coerceIn(0, plainText.length)
        val anchor = anchors.lastOrNull { clamped >= it.globalStartOffset } ?: anchors.first()
        val blockCharOffset = (clamped - anchor.globalStartOffset).coerceIn(0, anchor.textLength)
        return ReadingPosition(
            sectionId = anchor.sectionId,
            blockIndex = anchor.blockIndex,
            blockCharOffset = blockCharOffset,
            globalCharOffset = anchor.globalStartOffset + blockCharOffset,
        )
    }

    fun locateFromSearch(title: String, searchCharOffset: Int): ReadingPosition? {
        if (anchors.isEmpty()) return null
        if (searchCharOffset <= title.length) {
            return locateGlobalOffset(0)
        }
        val bodyOffset = searchCharOffset - title.length - 1
        if (bodyOffset < 0) {
            return locateGlobalOffset(0)
        }
        return locateGlobalOffset(bodyOffset)
    }

    fun locateFromProgress(
        sectionId: String?,
        blockIndex: Int,
        blockCharOffset: Int,
        globalCharOffset: Int,
    ): ReadingPosition? {
        if (sectionId != null) {
            val anchor =
                anchors.firstOrNull {
                    it.sectionId == sectionId && it.blockIndex == blockIndex
                }
            if (anchor != null) {
                val clampedBlockOffset = blockCharOffset.coerceIn(0, anchor.textLength)
                return ReadingPosition(
                    sectionId = sectionId,
                    blockIndex = blockIndex,
                    blockCharOffset = clampedBlockOffset,
                    globalCharOffset = anchor.globalStartOffset + clampedBlockOffset,
                )
            }
        }
        return locateGlobalOffset(globalCharOffset)
    }

    fun readerBlockIndex(blocks: List<ReaderBlock>, position: ReadingPosition): Int = blocks
        .indexOfFirst {
            it.sectionId == position.sectionId && it.blockIndex == position.blockIndex
        }.coerceAtLeast(0)

    companion object {
        fun fromDocumentPackage(documentPackage: DocumentPackage): PlainTextIndex {
            val anchors = mutableListOf<BlockAnchor>()
            val text = StringBuilder()
            for (sectionId in documentPackage.sectionOrder) {
                val blocks = documentPackage.sections[sectionId].orEmpty()
                blocks.forEachIndexed { blockIndex, block ->
                    val blockText = block.plainText()
                    if (blockText.isNotBlank()) {
                        anchors +=
                            BlockAnchor(
                                sectionId = sectionId,
                                blockIndex = blockIndex,
                                globalStartOffset = text.length,
                                textLength = blockText.length,
                            )
                        text.append(blockText).append('\n')
                    }
                }
            }
            val plainText = text.toString().trim()
            val trimmedLengthDelta = text.length - plainText.length
            val adjustedAnchors =
                if (trimmedLengthDelta > 0) {
                    anchors.mapNotNull { anchor ->
                        if (anchor.globalStartOffset >= plainText.length) {
                            null
                        } else {
                            val maxLength =
                                (plainText.length - anchor.globalStartOffset)
                                    .coerceAtMost(anchor.textLength)
                            anchor.copy(textLength = maxLength)
                        }
                    }
                } else {
                    anchors
                }
            return PlainTextIndex(plainText, adjustedAnchors)
        }
    }
}
