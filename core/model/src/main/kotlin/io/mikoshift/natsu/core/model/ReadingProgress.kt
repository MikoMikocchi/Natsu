package io.mikoshift.natsu.core.model

data class ReadingProgress(
    val documentId: String,
    val lastReadCharOffset: Int = 0,
    val lastReadSectionId: String? = null,
    val lastReadBlockIndex: Int = 0,
    val lastReadBlockCharOffset: Int = 0,
    val updatedAtMs: Long = 0,
    val clientUpdatedAtMs: Long = 0,
)
