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
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResult as DocumentSearchResultDto
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.DocumentSyncItemRequest
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

fun DocumentResponse.toDocumentEntity(): DocumentEntity = DocumentEntity(
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

fun DocumentResponse.toProgressEntity(): ReadingProgressEntity = ReadingProgressEntity(
    documentId = id,
    lastReadCharOffset = lastReadCharOffset,
    lastReadSectionId = lastReadSectionId,
    lastReadBlockIndex = lastReadBlockIndex,
    lastReadBlockCharOffset = lastReadBlockCharOffset,
    updatedAtMs = updatedAtMs,
    clientUpdatedAtMs = updatedAtMs,
)

fun DocumentResponse.toEntities(): Pair<DocumentEntity, ReadingProgressEntity> =
    toDocumentEntity() to toProgressEntity()

fun DocumentResponse.toEntity(): DocumentEntity = toDocumentEntity()

fun DocumentResponse.toDomain(
    progress: ReadingProgress? = null,
    cache: DocumentCache? = null,
): Document = Document(
    metadata = toDocumentEntity().toDomain(),
    progress = progress,
    cache = cache,
)

fun DocumentEntity.toSyncItemRequest(
    progress: ReadingProgressEntity?,
    idempotencyKey: String,
): DocumentSyncItemRequest {
    val progressEntity = progress ?: ReadingProgressEntity(
        documentId = id,
        lastReadCharOffset = 0,
        lastReadSectionId = null,
        lastReadBlockIndex = 0,
        lastReadBlockCharOffset = 0,
        updatedAtMs = 0,
        clientUpdatedAtMs = 0,
    )
    return DocumentSyncItemRequest(
        id = id,
        idempotencyKey = idempotencyKey,
        title = title,
        sourceFormat = sourceFormat.toDto(),
        importedAt = importedAt,
        charCount = charCount,
        lastReadCharOffset = progressEntity.lastReadCharOffset,
        lastReadSectionId = progressEntity.lastReadSectionId,
        lastReadBlockIndex = progressEntity.lastReadBlockIndex,
        lastReadBlockCharOffset = progressEntity.lastReadBlockCharOffset,
        updatedAtMs = maxOf(updatedAtMs, progressEntity.updatedAtMs),
        deleted = deleted,
    )
}

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
