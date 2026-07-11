package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.remote.dto.DocumentSyncItemRequest
import io.mikoshift.natsu.data.remote.dto.SourceFormat

@Entity(
    tableName = "documents",
    indices = [Index(value = ["updatedAtMs"])],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
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

fun DocumentResponse.toEntity(
    isDirty: Boolean = false,
    localPackagePath: String? = null,
    cachedPackageSha256: String? = null,
): DocumentEntity = DocumentEntity(
    id = id,
    title = title,
    sourceFormat = sourceFormat,
    status = status,
    importError = importError,
    importedAt = importedAt,
    charCount = charCount,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    packageSizeBytes = packageSizeBytes,
    packageUpdatedAtMs = packageUpdatedAtMs,
    packageSha256 = packageSha256,
    deleted = deleted,
    isDirty = isDirty,
    localPackagePath = localPackagePath,
    cachedPackageSha256 = cachedPackageSha256,
)

fun DocumentEntity.toSyncItemRequest(): DocumentSyncItemRequest = DocumentSyncItemRequest(
    id = id,
    title = title,
    sourceFormat = sourceFormat,
    importedAt = importedAt,
    charCount = charCount,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    deleted = deleted,
)
