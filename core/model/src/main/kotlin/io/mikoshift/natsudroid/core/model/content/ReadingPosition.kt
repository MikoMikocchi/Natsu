package io.mikoshift.natsudroid.core.model.content

data class ReadingPosition(
    val sectionId: String,
    val blockIndex: Int,
    val blockCharOffset: Int,
    val globalCharOffset: Int,
)
