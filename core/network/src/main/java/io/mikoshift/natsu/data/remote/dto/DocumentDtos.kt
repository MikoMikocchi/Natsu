package io.mikoshift.natsu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SourceFormat {
    @SerialName("EPUB") EPUB,
    @SerialName("MARKDOWN") MARKDOWN,
    @SerialName("PLAIN_TEXT") PLAIN_TEXT,
    @SerialName("FB2") FB2,
    @SerialName("DOCX") DOCX,
    @SerialName("RTF") RTF,
}

@Serializable
enum class DocumentStatus {
    @SerialName("PENDING") PENDING,
    @SerialName("READY") READY,
    @SerialName("FAILED") FAILED,
}

@Serializable
data class DocumentResponse(
    val id: String,
    val title: String,
    @SerialName("source_format") val sourceFormat: SourceFormat,
    val status: DocumentStatus,
    @SerialName("import_error") val importError: String? = null,
    @SerialName("imported_at") val importedAt: Long = 0,
    @SerialName("char_count") val charCount: Int = 0,
    @SerialName("last_read_char_offset") val lastReadCharOffset: Int = 0,
    @SerialName("last_read_section_id") val lastReadSectionId: String? = null,
    @SerialName("last_read_block_index") val lastReadBlockIndex: Int = 0,
    @SerialName("last_read_block_char_offset") val lastReadBlockCharOffset: Int = 0,
    @SerialName("updated_at_ms") val updatedAtMs: Long = 0,
    @SerialName("package_size_bytes") val packageSizeBytes: Long = 0,
    @SerialName("package_updated_at_ms") val packageUpdatedAtMs: Long = 0,
    @SerialName("package_sha256") val packageSha256: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class DocumentIndexResponse(
    val documents: List<DocumentResponse>,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class DocumentShowResponse(
    val document: DocumentResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class DocumentSyncItemRequest(
    val id: String,
    @SerialName("idempotency_key") val idempotencyKey: String,
    val title: String? = null,
    @SerialName("source_format") val sourceFormat: SourceFormat,
    @SerialName("imported_at") val importedAt: Long,
    @SerialName("char_count") val charCount: Int,
    @SerialName("last_read_char_offset") val lastReadCharOffset: Int,
    @SerialName("last_read_section_id") val lastReadSectionId: String? = null,
    @SerialName("last_read_block_index") val lastReadBlockIndex: Int,
    @SerialName("last_read_block_char_offset") val lastReadBlockCharOffset: Int,
    @SerialName("updated_at_ms") val updatedAtMs: Long,
    val deleted: Boolean,
)

@Serializable
data class DocumentSyncRequest(
    val documents: List<DocumentSyncItemRequest>,
)

@Serializable
data class DocumentSearchMatch(
    @SerialName("char_offset") val charOffset: Int,
    val snippet: String,
)

@Serializable
data class DocumentSearchResult(
    val id: String,
    val title: String,
    val matches: List<DocumentSearchMatch>,
)

@Serializable
data class DocumentSearchResponse(
    val results: List<DocumentSearchResult>,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)
