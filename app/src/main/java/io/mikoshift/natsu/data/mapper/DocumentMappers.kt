package io.mikoshift.natsu.data.mapper

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentSearchMatch
import io.mikoshift.natsu.core.model.DocumentSearchResult
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResult as DocumentSearchResultDto
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.SourceFormat as SourceFormatDto

fun DocumentEntity.toDomain(): Document = Document(
    id = id,
    title = title,
    sourceFormat = sourceFormat.toDomain(),
    status = status.toDomain(),
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

fun DocumentResponse.toDomain(
    isDirty: Boolean = false,
    localPackagePath: String? = null,
    cachedPackageSha256: String? = null,
): Document = Document(
    id = id,
    title = title,
    sourceFormat = sourceFormat.toDomain(),
    status = status.toDomain(),
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
