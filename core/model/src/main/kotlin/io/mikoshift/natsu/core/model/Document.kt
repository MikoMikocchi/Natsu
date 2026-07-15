package io.mikoshift.natsu.core.model

enum class SourceFormat {
    EPUB,
    MARKDOWN,
    PLAIN_TEXT,
}

enum class DocumentStatus {
    PENDING,
    READY,
    FAILED,
}

data class Document(
    val id: String,
    val title: String,
    val sourceFormat: SourceFormat,
    val status: DocumentStatus,
    val importError: String? = null,
    val importedAt: Long = 0,
    val charCount: Int = 0,
    val lastReadCharOffset: Int = 0,
    val lastReadSectionId: String? = null,
    val lastReadBlockIndex: Int = 0,
    val lastReadBlockCharOffset: Int = 0,
    val updatedAtMs: Long = 0,
    val packageSizeBytes: Long = 0,
    val packageUpdatedAtMs: Long = 0,
    val packageSha256: String? = null,
    val deleted: Boolean = false,
    val isDirty: Boolean = false,
    val localPackagePath: String? = null,
    val cachedPackageSha256: String? = null,
)

data class DocumentSearchMatch(
    val charOffset: Int,
    val snippet: String,
)

data class DocumentSearchResult(
    val id: String,
    val title: String,
    val matches: List<DocumentSearchMatch>,
)
