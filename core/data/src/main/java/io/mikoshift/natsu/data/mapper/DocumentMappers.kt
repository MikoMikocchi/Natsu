package io.mikoshift.natsu.data.mapper

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentCache
import io.mikoshift.natsu.core.model.DocumentMetadata
import io.mikoshift.natsu.core.model.DocumentSearchMatch
import io.mikoshift.natsu.core.model.DocumentSearchResult
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.ReadingProgress
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.data.local.db.DocumentCacheEntity
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.DocumentWithRelations
import io.mikoshift.natsu.data.local.db.ReadingProgressEntity
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataSyncItemRequest
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResult as DocumentSearchResultDto
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.ReadingProgressResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressSyncItemRequest
import io.mikoshift.natsu.data.remote.dto.SourceFormat as SourceFormatDto

fun DocumentWithRelations.toDomain(): Document = Document(
    metadata = document.toDomain(),
    progress = progress?.toDomain(),
    cache = cache?.toDomain(),
)

fun DocumentEntity.toDomain(): DocumentMetadata = DocumentMetadata(
    id = id,
    title = title,
    sourceFormat = sourceFormat,
    status = status,
    importError = importError,
    importedAt = importedAt,
    charCount = charCount,
    updatedAtMs = updatedAtMs,
    packageSizeBytes = packageSizeBytes,
    packageUpdatedAtMs = packageUpdatedAtMs,
    packageSha256 = packageSha256,
    deleted = deleted,
)

fun ReadingProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    documentId = documentId,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    clientUpdatedAtMs = clientUpdatedAtMs,
)

fun DocumentCacheEntity.toDomain(): DocumentCache = DocumentCache(
    documentId = documentId,
    localPackagePath = localPackagePath,
    cachedPackageSha256 = cachedPackageSha256,
)

fun DocumentMetadataResponse.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    title = title,
    sourceFormat = sourceFormat.toDomain(),
    status = status.toDomain(),
    importError = importError,
    importedAt = importedAt,
    charCount = charCount,
    updatedAtMs = updatedAtMs,
    packageSizeBytes = packageSizeBytes,
    packageUpdatedAtMs = packageUpdatedAtMs,
    packageSha256 = packageSha256,
    deleted = deleted,
)

fun ReadingProgressResponse.toEntity(): ReadingProgressEntity = ReadingProgressEntity(
    documentId = documentId,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    clientUpdatedAtMs = clientUpdatedAtMs,
)

fun DocumentMetadataResponse.toDomain(
    progress: ReadingProgress? = null,
    cache: DocumentCache? = null,
): Document = Document(
    metadata = toEntity().toDomain(),
    progress = progress,
    cache = cache,
)

fun DocumentEntity.toSyncItemRequest(): DocumentMetadataSyncItemRequest = DocumentMetadataSyncItemRequest(
    id = id,
    title = title,
    sourceFormat = sourceFormat.toDto(),
    importedAt = importedAt,
    charCount = charCount,
    updatedAtMs = updatedAtMs,
    deleted = deleted,
)

fun ReadingProgressEntity.toSyncItemRequest(): ReadingProgressSyncItemRequest = ReadingProgressSyncItemRequest(
    documentId = documentId,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    clientUpdatedAtMs = clientUpdatedAtMs,
)

fun DocumentSearchResultDto.toDomain(): DocumentSearchResult = DocumentSearchResult(
    id = id,
    title = title,
    matches = matches.map { match ->
        DocumentSearchMatch(
            charOffset = match.charOffset,
            snippet = match.snippet,
        )
    },
)

fun SourceFormatDto.toDomain(): SourceFormat = SourceFormat.valueOf(name)

fun DocumentStatusDto.toDomain(): DocumentStatus = DocumentStatus.valueOf(name)

fun SourceFormat.toDto(): SourceFormatDto = SourceFormatDto.valueOf(name)

fun DocumentStatus.toDto(): DocumentStatusDto = DocumentStatusDto.valueOf(name)
