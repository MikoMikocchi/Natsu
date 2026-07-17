package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.mapper.toDomain
import io.mikoshift.natsu.data.mapper.toEntity
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse

object MetadataMerger {

    fun merge(
        server: DocumentMetadataResponse,
        local: DocumentEntity?,
        hasPendingOutbox: Boolean,
    ): DocumentEntity {
        if (local == null) {
            return server.toEntity()
        }

        if (server.updatedAtMs > local.updatedAtMs) {
            return server.toEntity()
        }

        if (hasPendingOutbox) {
            return local.copy(
                status = server.status.toDomain(),
                importError = server.importError,
                packageSizeBytes = server.packageSizeBytes,
                packageUpdatedAtMs = server.packageUpdatedAtMs,
                packageSha256 = server.packageSha256,
                charCount = if (server.charCount > 0) server.charCount else local.charCount,
            )
        }

        return server.toEntity()
    }
}
