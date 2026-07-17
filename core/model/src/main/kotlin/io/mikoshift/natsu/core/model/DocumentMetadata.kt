package io.mikoshift.natsu.core.model

data class DocumentMetadata(
    val id: String,
    val title: String,
    val sourceFormat: SourceFormat,
    val status: DocumentStatus,
    val importError: String? = null,
    val importedAt: Long = 0,
    val charCount: Int = 0,
    val updatedAtMs: Long = 0,
    val packageSizeBytes: Long = 0,
    val packageUpdatedAtMs: Long = 0,
    val packageSha256: String? = null,
    val deleted: Boolean = false,
)
