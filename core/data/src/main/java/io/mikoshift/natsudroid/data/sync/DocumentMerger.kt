package io.mikoshift.natsudroid.data.sync

import io.mikoshift.natsudroid.data.local.db.DocumentEntity
import io.mikoshift.natsudroid.data.local.db.ReadingProgressEntity
import io.mikoshift.natsudroid.data.mapper.toDocumentEntity
import io.mikoshift.natsudroid.data.mapper.toProgressEntity
import io.mikoshift.natsudroid.data.remote.dto.DocumentResponse

object DocumentMerger {
    fun merge(
        server: DocumentResponse,
        localDocument: DocumentEntity?,
        localProgress: ReadingProgressEntity?,
        hasPendingMetadata: Boolean,
        hasPendingProgress: Boolean,
    ): Pair<DocumentEntity, ReadingProgressEntity> {
        val serverDocument = server.toDocumentEntity()
        val serverProgress = server.toProgressEntity()

        if (localDocument == null) {
            return serverDocument to serverProgress
        }

        val localDocumentTs = localDocument.updatedAtMs
        val localProgressTs = localProgress?.updatedAtMs ?: 0L
        val localMaxTs = maxOf(localDocumentTs, localProgressTs)

        if (server.updatedAtMs > localMaxTs) {
            return serverDocument to serverProgress
        }

        val mergedDocument =
            when {
                hasPendingMetadata ->
                    localDocument.copy(
                        status = serverDocument.status,
                        importError = serverDocument.importError,
                        packageSizeBytes = serverDocument.packageSizeBytes,
                        packageUpdatedAtMs = serverDocument.packageUpdatedAtMs,
                        packageSha256 = serverDocument.packageSha256,
                        charCount =
                        if (serverDocument.charCount > 0) {
                            serverDocument.charCount
                        } else {
                            localDocument.charCount
                        },
                    )

                else -> serverDocument
            }

        val mergedProgress =
            when {
                hasPendingProgress && localProgress != null -> localProgress
                hasPendingProgress -> serverProgress
                else -> serverProgress
            }

        return mergedDocument to mergedProgress
    }
}
