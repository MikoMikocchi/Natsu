package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.toEntity
import io.mikoshift.natsu.data.remote.dto.DocumentResponse

object DocumentMerger {

    fun merge(server: DocumentResponse, local: DocumentEntity?): DocumentEntity {
        if (local == null) {
            return server.toEntity()
        }

        if (server.updatedAtMs > local.updatedAtMs) {
            val keepPackage = server.packageSha256 != null &&
                server.packageSha256 == local.cachedPackageSha256
            return server.toEntity(
                isDirty = false,
                localPackagePath = if (keepPackage) local.localPackagePath else null,
                cachedPackageSha256 = if (keepPackage) local.cachedPackageSha256 else null,
            )
        }

        if (local.isDirty) {
            return local.copy(
                status = server.status,
                importError = server.importError,
                packageSizeBytes = server.packageSizeBytes,
                packageUpdatedAtMs = server.packageUpdatedAtMs,
                packageSha256 = server.packageSha256,
                charCount = if (server.charCount > 0) server.charCount else local.charCount,
            )
        }

        val keepPackage = server.packageSha256 != null &&
            server.packageSha256 == local.cachedPackageSha256
        return server.toEntity(
            isDirty = false,
            localPackagePath = if (keepPackage) local.localPackagePath else null,
            cachedPackageSha256 = if (keepPackage) local.cachedPackageSha256 else null,
        )
    }
}
