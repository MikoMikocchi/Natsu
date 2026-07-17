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
import io.mikoshift.natsu.data.mapper.toEntity
import io.mikoshift.natsu.data.mapper.toSyncItemRequest
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataSyncRequest
import io.mikoshift.natsu.data.remote.dto.ReadingProgressIndexResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressSyncRequest
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
        val metadataCursor = pullMetadata()
        val progressCursor = pullProgress()
        pushOutbox()
        packageDownloadService.downloadMissingPackages()
        metadataCursor?.let { syncCursorStore.setMetadataSinceMs(it) }
        progressCursor?.let { syncCursorStore.setProgressSinceMs(it) }
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

    private suspend fun pullMetadata(): Long? {
        val since = syncCursorStore.getMetadataSinceMs()
        val response = documentApi.indexMetadata(since = since)
        val body = requireSuccess(response)
        var maxUpdatedAtMs = since

        for (serverDoc in body.documents) {
            val local = documentDao.getById(serverDoc.id)
            val hasPending = syncOutboxStore.hasPendingMetadata(serverDoc.id)
            val merged = MetadataMerger.merge(serverDoc, local, hasPending)
            documentDao.upsert(merged)

            if (serverDoc.updatedAtMs > maxUpdatedAtMs) {
                maxUpdatedAtMs = serverDoc.updatedAtMs
            }

            if (serverDoc.updatedAtMs > (local?.updatedAtMs ?: 0L)) {
                syncOutboxStore.clearEntity(SyncEntityType.METADATA, serverDoc.id)
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

    private suspend fun pullProgress(): Long? {
        val since = syncCursorStore.getProgressSinceMs()
        val response = documentApi.indexProgress(since = since)
        val body = requireSuccess(response)
        var maxUpdatedAtMs = since

        for (serverProgress in body.progress) {
            val local = readingProgressDao.getByDocumentId(serverProgress.documentId)
            val hasPending = syncOutboxStore.hasPendingProgress(serverProgress.documentId)
            val merged = ProgressMerger.merge(serverProgress, local, hasPending)
            readingProgressDao.upsert(merged)

            if (serverProgress.updatedAtMs > maxUpdatedAtMs) {
                maxUpdatedAtMs = serverProgress.updatedAtMs
            }

            if (serverProgress.updatedAtMs > (local?.updatedAtMs ?: 0L)) {
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverProgress.documentId)
            }
        }

        val newCursor = maxOf(maxUpdatedAtMs, body.serverTimeMs)
        return newCursor.takeIf { it > since }
    }

    private suspend fun pushOutbox() {
        syncOutboxDao.resetInProgressToPending()

        val pending = syncOutboxDao.getPending()
        if (pending.isEmpty()) return

        val metadataEntries = pending.filter { it.entityType == SyncEntityType.METADATA }
        val progressEntries = pending.filter { it.entityType == SyncEntityType.PROGRESS }

        pushMetadata(metadataEntries)
        pushProgress(progressEntries)
    }

    private suspend fun pushMetadata(entries: List<SyncOutboxEntity>) {
        if (entries.isEmpty()) return

        entries.chunked(MAX_SYNC_BATCH).forEach { batch ->
            pushMetadataBatch(batch)
        }
    }

    private suspend fun pushMetadataBatch(batch: List<SyncOutboxEntity>) {
        val attemptsById = batch.associate { entry ->
            entry.id to entry.attempts + 1
        }
        batch.forEach { entry ->
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.IN_PROGRESS,
                attempts = attemptsById.getValue(entry.id),
                lastError = null,
            )
        }

        val (syncedEntries, orphanedEntries) = batch.partition { entry ->
            documentDao.getById(entry.entityId) != null
        }
        orphanedEntries.forEach { entry ->
            syncOutboxStore.clearEntity(SyncEntityType.METADATA, entry.entityId)
        }
        if (syncedEntries.isEmpty()) return

        val request = DocumentMetadataSyncRequest(
            documents = syncedEntries.mapNotNull { entry ->
                documentDao.getById(entry.entityId)?.toSyncItemRequest()
            },
        )

        try {
            // Push is idempotent: the server resolves conflicts by updatedAtMs (LWW).
            val response = documentApi.syncMetadata(request)
            val body = requireSuccess(response)
            applyMetadataSyncResponse(body)
            syncedEntries.forEach { entry ->
                syncOutboxStore.clearEntity(SyncEntityType.METADATA, entry.entityId)
            }
        } catch (error: Exception) {
            markBatchFailed(syncedEntries, attemptsById, error)
            throw error
        }
    }

    private suspend fun pushProgress(entries: List<SyncOutboxEntity>) {
        if (entries.isEmpty()) return

        entries.chunked(MAX_SYNC_BATCH).forEach { batch ->
            pushProgressBatch(batch)
        }
    }

    private suspend fun pushProgressBatch(batch: List<SyncOutboxEntity>) {
        val attemptsById = batch.associate { entry ->
            entry.id to entry.attempts + 1
        }
        batch.forEach { entry ->
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.IN_PROGRESS,
                attempts = attemptsById.getValue(entry.id),
                lastError = null,
            )
        }

        val (syncedEntries, orphanedEntries) = batch.partition { entry ->
            readingProgressDao.getByDocumentId(entry.entityId) != null
        }
        orphanedEntries.forEach { entry ->
            syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, entry.entityId)
        }
        if (syncedEntries.isEmpty()) return

        val request = ReadingProgressSyncRequest(
            progress = syncedEntries.mapNotNull { entry ->
                readingProgressDao.getByDocumentId(entry.entityId)?.toSyncItemRequest()
            },
        )

        try {
            // Push is idempotent: the server resolves conflicts by updatedAtMs (LWW).
            val response = documentApi.syncProgress(request)
            val body = requireSuccess(response)
            applyProgressSyncResponse(body)
            syncedEntries.forEach { entry ->
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, entry.entityId)
            }
        } catch (error: Exception) {
            markBatchFailed(syncedEntries, attemptsById, error)
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

    private suspend fun applyMetadataSyncResponse(body: DocumentMetadataIndexResponse) {
        for (serverDoc in body.documents) {
            val localCache = documentCacheDao.getByDocumentId(serverDoc.id)
            val keepPackage = serverDoc.packageSha256 != null &&
                serverDoc.packageSha256 == localCache?.cachedPackageSha256

            documentDao.upsert(serverDoc.toEntity())

            if (keepPackage && localCache != null) {
                documentCacheDao.upsert(localCache)
            } else if (localCache != null && !keepPackage) {
                documentCacheDao.deleteByDocumentId(serverDoc.id)
            }

            syncOutboxStore.clearEntity(SyncEntityType.METADATA, serverDoc.id)

            if (serverDoc.deleted) {
                readingProgressDao.deleteByDocumentId(serverDoc.id)
                documentCacheDao.deleteByDocumentId(serverDoc.id)
                syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverDoc.id)
                packageFileStore.delete(serverDoc.id)
            }
        }
    }

    private suspend fun applyProgressSyncResponse(body: ReadingProgressIndexResponse) {
        for (serverProgress in body.progress) {
            readingProgressDao.upsert(serverProgress.toEntity())
            syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, serverProgress.documentId)
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
