package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.toEntity
import io.mikoshift.natsu.data.local.db.toSyncItemRequest
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSyncRequest
import io.mikoshift.natsu.data.repository.DocumentError
import java.io.IOException
import retrofit2.Response

class DocumentSyncEngine(
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    private val syncCursorStore: SyncCursorStore,
    private val packageFileStore: PackageFileStore,
) {

    suspend fun sync(): Result<Unit> = runCatching {
        pullRemoteChanges()
        pushLocalChanges()
        downloadMissingPackages()
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { throwable ->
            Result.failure(
                if (throwable is DocumentError) throwable
                else if (throwable is IOException) DocumentError.NetworkFailure
                else DocumentError.Unknown(throwable.message),
            )
        },
    )

    private suspend fun pullRemoteChanges() {
        val since = syncCursorStore.getLastSinceMs()
        val response = documentApi.index(since = since)
        val body = requireSuccess(response)
        var maxUpdatedAtMs = since

        for (serverDoc in body.documents) {
            val local = documentDao.getById(serverDoc.id)
            val merged = mergeServerDocument(serverDoc, local)
            documentDao.upsert(merged)
            if (serverDoc.updatedAtMs > maxUpdatedAtMs) {
                maxUpdatedAtMs = serverDoc.updatedAtMs
            }
            if (serverDoc.deleted) {
                packageFileStore.delete(serverDoc.id)
            }
        }

        val newCursor = maxOf(maxUpdatedAtMs, body.serverTimeMs)
        if (newCursor > since) {
            syncCursorStore.setLastSinceMs(newCursor)
        }
    }

    private suspend fun pushLocalChanges() {
        val dirtyDocuments = documentDao.getDirtyDocuments()
        if (dirtyDocuments.isEmpty()) return

        dirtyDocuments.chunked(MAX_SYNC_BATCH).forEach { batch ->
            val request = DocumentSyncRequest(
                documents = batch.map { it.toSyncItemRequest() },
            )
            val response = documentApi.sync(request)
            val body = requireSuccess(response)
            applySyncResponse(body)
        }
    }

    private suspend fun applySyncResponse(body: DocumentIndexResponse) {
        for (serverDoc in body.documents) {
            val local = documentDao.getById(serverDoc.id)
            val keepPackage = serverDoc.packageSha256 != null &&
                serverDoc.packageSha256 == local?.cachedPackageSha256
            documentDao.upsert(
                serverDoc.toEntity(
                    isDirty = false,
                    localPackagePath = if (keepPackage) local?.localPackagePath else null,
                    cachedPackageSha256 = if (keepPackage) local?.cachedPackageSha256 else null,
                ),
            )
            if (serverDoc.deleted) {
                packageFileStore.delete(serverDoc.id)
            }
        }
    }

    private suspend fun downloadMissingPackages() {
        val pending = documentDao.getDocumentsNeedingPackageDownload()
        for (document in pending) {
            val sha256 = document.packageSha256 ?: continue
            val response = documentApi.downloadPackage(document.id)
            if (!response.isSuccessful) {
                throw mapErrorResponse(response)
            }
            val body = response.body() ?: throw DocumentError.Unknown("Empty package response")
            try {
                val path = packageFileStore.save(document.id, body)
                documentDao.upsert(
                    document.copy(
                        localPackagePath = path,
                        cachedPackageSha256 = sha256,
                    ),
                )
            } finally {
                body.close()
            }
        }
    }

    private fun mergeServerDocument(
        server: DocumentResponse,
        local: DocumentEntity?,
    ): DocumentEntity {
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

    private fun <T> requireSuccess(response: Response<T>): T {
        val body = response.body()
        if (response.isSuccessful && body != null) {
            return body
        }
        throw mapErrorResponse(response)
    }

    private fun <T> mapErrorResponse(response: Response<T>): DocumentError =
        if (response.code() == 401) {
            DocumentError.Unauthorized
        } else {
            DocumentError.Unknown(response.message())
        }

    private companion object {
        const val MAX_SYNC_BATCH = 100
    }
}
