package io.mikoshift.natsudroid.data.sync

import io.mikoshift.natsudroid.core.model.DocumentError
import io.mikoshift.natsudroid.data.local.PackageFileStore
import io.mikoshift.natsudroid.data.local.SyncCursorStore
import io.mikoshift.natsudroid.data.local.SyncOutboxStore
import io.mikoshift.natsudroid.data.local.db.DocumentCacheDao
import io.mikoshift.natsudroid.data.local.db.DocumentDao
import io.mikoshift.natsudroid.data.local.db.ReadingProgressDao
import io.mikoshift.natsudroid.data.local.db.SyncEntityType
import io.mikoshift.natsudroid.data.local.db.SyncOutboxDao
import io.mikoshift.natsudroid.data.local.db.SyncOutboxEntity
import io.mikoshift.natsudroid.data.local.db.SyncOutboxStatus
import io.mikoshift.natsudroid.data.mapper.toEntities
import io.mikoshift.natsudroid.data.mapper.toSyncItemRequest
import io.mikoshift.natsudroid.data.remote.DocumentApi
import io.mikoshift.natsudroid.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentSyncRequest
import retrofit2.Response
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentSyncEngine
@Inject
constructor(
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
                if (throwable is DocumentError) {
                    throwable
                } else if (throwable is IOException) {
                    DocumentError.NetworkFailure
                } else {
                    DocumentError.Unknown(throwable.message)
                },
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
            val (mergedDocument, mergedProgress) =
                DocumentMerger.merge(
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

            val localMaxTs =
                maxOf(
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

    private suspend fun pushDocumentsBatch(documentIds: List<String>, pending: List<SyncOutboxEntity>) {
        val batchEntries = pending.filter { it.entityId in documentIds }
        val attemptsById =
            batchEntries.associate { entry ->
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

        val syncItems =
            documentIds.mapNotNull { documentId ->
                val document =
                    documentDao.getById(documentId) ?: run {
                        syncOutboxStore.clearEntity(SyncEntityType.METADATA, documentId)
                        syncOutboxStore.clearEntity(SyncEntityType.PROGRESS, documentId)
                        return@mapNotNull null
                    }
                val progress = readingProgressDao.getByDocumentId(documentId)
                val itemIdempotencyKey = itemIdempotencyKey(batchEntries, documentId)
                document.toSyncItemRequest(progress, itemIdempotencyKey)
            }

        if (syncItems.isEmpty()) return

        val request = DocumentSyncRequest(documents = syncItems)
        val batchIdempotencyKey = batchIdempotencyKey(batchEntries)

        try {
            val response = documentApi.syncDocuments(batchIdempotencyKey, request)
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
            val keepPackage =
                serverDoc.packageSha256 != null &&
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

    private fun <T> mapErrorResponse(response: Response<T>): DocumentError = if (response.code() == 401) {
        DocumentError.Unauthorized
    } else {
        DocumentError.Unknown(response.message())
    }

    private companion object {
        const val MAX_SYNC_BATCH = 100
        const val MAX_IDEMPOTENCY_KEY_LENGTH = 255

        fun itemIdempotencyKey(entries: List<SyncOutboxEntity>, documentId: String): String {
            val keys = entries.filter { it.entityId == documentId }.map { it.idempotencyKey }.sorted()
            require(keys.isNotEmpty()) { "Missing outbox idempotency key for document $documentId" }
            return when {
                keys.size == 1 -> keys.single()
                else -> sha256Hex(keys.joinToString("|"))
            }
        }

        fun batchIdempotencyKey(entries: List<SyncOutboxEntity>): String {
            val composite = entries.map { it.idempotencyKey }.sorted().joinToString("|")
            return if (composite.length <= MAX_IDEMPOTENCY_KEY_LENGTH) {
                composite
            } else {
                sha256Hex(composite)
            }
        }

        private fun sha256Hex(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            return digest.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
