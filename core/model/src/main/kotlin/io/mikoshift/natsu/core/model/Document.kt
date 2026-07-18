package io.mikoshift.natsu.core.model

enum class SourceFormat {
    EPUB,
    MARKDOWN,
    PLAIN_TEXT,
    FB2,
    DOCX,
    RTF,
}

enum class DocumentStatus {
    PENDING,
    READY,
    FAILED,
}

data class Document(
    val metadata: DocumentMetadata,
    val progress: ReadingProgress? = null,
    val cache: DocumentCache? = null,
) {
    val id: String get() = metadata.id
    val title: String get() = metadata.title
    val sourceFormat: SourceFormat get() = metadata.sourceFormat
    val status: DocumentStatus get() = metadata.status
    val importError: String? get() = metadata.importError
    val importedAt: Long get() = metadata.importedAt
    val charCount: Int get() = metadata.charCount
    val updatedAtMs: Long get() = metadata.updatedAtMs
    val packageSizeBytes: Long get() = metadata.packageSizeBytes
    val packageUpdatedAtMs: Long get() = metadata.packageUpdatedAtMs
    val packageSha256: String? get() = metadata.packageSha256
    val deleted: Boolean get() = metadata.deleted
    val lastReadCharOffset: Int get() = progress?.lastReadCharOffset ?: 0
    val lastReadSectionId: String? get() = progress?.lastReadSectionId
    val lastReadBlockIndex: Int get() = progress?.lastReadBlockIndex ?: 0
    val lastReadBlockCharOffset: Int get() = progress?.lastReadBlockCharOffset ?: 0
    val progressUpdatedAtMs: Long get() = progress?.updatedAtMs ?: 0
    val localPackagePath: String? get() = cache?.localPackagePath
    val cachedPackageSha256: String? get() = cache?.cachedPackageSha256
}

data class DocumentSearchMatch(
    val charOffset: Int,
    val snippet: String,
)

data class DocumentSearchResult(
    val id: String,
    val title: String,
    val matches: List<DocumentSearchMatch>,
)
