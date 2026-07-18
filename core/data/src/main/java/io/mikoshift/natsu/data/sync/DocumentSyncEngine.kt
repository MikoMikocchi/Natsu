package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.SyncOutboxStore
import io.mikoshift.natsu.data.local.db.DocumentCacheDao
import io.mikoshift.natsu.data.local.db.DocumentCacheEntity
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.ReadingProgressDao
import io.mikoshift.natsu.data.local.db.SyncEntityType
import io.mikoshift.natsu.data.local.db.SyncOutboxDao
import io.mikoshift.natsu.data.local.db.SyncOutboxEntity
import io.mikoshift.natsu.data.local.db.SyncOutboxStatus
import io.mikoshift.natsu.data.mapper.toEntities
import io.mikoshift.natsu.data.mapper.toSyncItemRequest
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSyncRequest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class DocumentSyncEngine @Inject constructor(
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    private val readingProgressDao: ReadingProgressDao,
    private val documentCacheDao: DocumentCacheDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncOutboxStore: SyncOutboxStore,
    private val syncCursorStore: SyncCursorStore,
    private val packageFileStore: PackageFileStore,
    private val packageDownloadService: PackageDownloadService,
) {

    suspend fun sync(): Result<Unit> = runCatching {
        val documentsCursor = pullDocuments()
        pushOutbox()
        packageDownloadService.downloadMissingPackages()
        documentsCursor?.let { syncCursorStore.setDocumentsSinceMs(it) }
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

    private suspend fun pullDocuments(): Long? {
        val since = syncCursorStore.getDocumentsSinceMs()
        val response = documentApi.indexDocuments(since = since)
        val body = requireSuccess(response)
        var maxUpdatedAtMs = since

        for (serverDoc in body.documents) {
            val localDocument = documentDao.getById(serverDoc.id)
            val localProgress = readingProgressDao.getByDocumentId(serverDoc.id)
            val hasPendingMetadata = syncOutboxStore.hasPendingMetadata(serverDoc.id)
            val hasPendingProgress = syncOutboxStore.hasPendingProgress(serverDoc.id)
            val (mergedDocument, mergedProgress) = DocumentMerger.merge(
                server = serverDoc,
                localDocument = localDocument,
                localProgress = localProgress,
                hasPendingMetadata = hasPendingMetadata,
                hasPendingProgress = hasPendingProgress,
            )
            documentDao.upsert(mergedDocument)
            readingProgressDao.upsert(mergedProgress)

            if (serverDoc.updatedAtMs > maxUpdatedAtMs) {
                maxUpdatedAtMs = serverDoc.updatedAtMs
            }

            val localMaxTs = maxOf(
                localDocument?.updatedAtMs ?: 0L,
                localProgress?.updatedAtMs ?: 0L,
            )
            if (serverDoc.updatedAtMs > localMaxTs) {
                syncOutboxStore.clearEntity(SyncEntityType.METADATA, serverDoc.id)
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverDoc.id)
            }

            if (serverDoc.deleted) {
                readingProgressDao.deleteByDocumentId(serverDoc.id)
                documentCacheDao.deleteByDocumentId(serverDoc.id)
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverDoc.id)
                packageFileStore.delete(serverDoc.id)
            }
        }

        val newCursor = maxOf(maxUpdatedAtMs, body.serverTimeMs)
        return newCursor.takeIf { it > since }
    }

    private suspend fun pushOutbox() {
        syncOutboxDao.resetInProgressToPending()

        val pending = syncOutboxDao.getPending()
        if (pending.isEmpty()) return

        val documentIds = pending.map { it.entityId }.distinct()
        documentIds.chunked(MAX_SYNC_BATCH).forEach { batchIds ->
            pushDocumentsBatch(batchIds, pending)
        }
    }

    private suspend fun pushDocumentsBatch(
        documentIds: List<String>,
        pending: List<SyncOutboxEntity>,
    ) {
        val batchEntries = pending.filter { it.entityId in documentIds }
        val attemptsById = batchEntries.associate { entry ->
            entry.id to entry.attempts + 1
        }
        batchEntries.forEach { entry ->
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.IN_PROGRESS,
                attempts = attemptsById.getValue(entry.id),
                lastError = null,
            )
        }

        val syncItems = documentIds.mapNotNull { documentId ->
            val document = documentDao.getById(documentId) ?: run {
                syncOutboxStore.clearEntity(SyncEntityType.METADATA, documentId)
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, documentId)
                return@mapNotNull null
            }
            val progress = readingProgressDao.getByDocumentId(documentId)
            document.toSyncItemRequest(progress)
        }

        if (syncItems.isEmpty()) return

        val request = DocumentSyncRequest(documents = syncItems)

        try {
            val response = documentApi.syncDocuments(request)
            val body = requireSuccess(response)
            applySyncResponse(body)
            batchEntries.forEach { entry ->
                syncOutboxStore.clearEntity(entry.entityType, entry.entityId)
            }
        } catch (error: Exception) {
            markBatchFailed(batchEntries, attemptsById, error)
            throw error
        }
    }

    private suspend fun markBatchFailed(
        entries: List<SyncOutboxEntity>,
        attemptsById: Map<String, Int>,
        error: Exception,
    ) {
        val message = error.message ?: error.javaClass.simpleName
        entries.forEach { entry ->
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.FAILED,
                attempts = attemptsById.getValue(entry.id),
                lastError = message,
            )
        }
    }

    private suspend fun applySyncResponse(body: DocumentIndexResponse) {
        for (serverDoc in body.documents) {
            val localCache = documentCacheDao.getByDocumentId(serverDoc.id)
            val keepPackage = serverDoc.packageSha256 != null &&
                serverDoc.packageSha256 == localCache?.cachedPackageSha256

            val (documentEntity, progressEntity) = serverDoc.toEntities()
            documentDao.upsert(documentEntity)
            readingProgressDao.upsert(progressEntity)

            if (keepPackage && localCache != null) {
                documentCacheDao.upsert(localCache)
            } else if (localCache != null && !keepPackage) {
                documentCacheDao.deleteByDocumentId(serverDoc.id)
            }

            syncOutboxStore.clearEntity(SyncEntityType.METADATA, serverDoc.id)
            syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverDoc.id)

            if (serverDoc.deleted) {
                readingProgressDao.deleteByDocumentId(serverDoc.id)
                documentCacheDao.deleteByDocumentId(serverDoc.id)
                packageFileStore.delete(serverDoc.id)
            }
        }
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
