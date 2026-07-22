package io.mikoshift.natsudroid.data.local

import io.mikoshift.natsudroid.data.local.db.SyncEntityType
import io.mikoshift.natsudroid.data.local.db.SyncOutboxDao
import io.mikoshift.natsudroid.data.local.db.SyncOutboxEntity
import io.mikoshift.natsudroid.data.local.db.SyncOutboxStatus
import java.util.UUID

class SyncOutboxStore(private val syncOutboxDao: SyncOutboxDao) {
    suspend fun enqueueMetadata(documentId: String, nowMs: Long = System.currentTimeMillis()) {
        syncOutboxDao.upsert(
            SyncOutboxEntity(
                id = outboxId(SyncEntityType.METADATA, documentId),
                entityType = SyncEntityType.METADATA,
                entityId = documentId,
                createdAtMs = nowMs,
                idempotencyKey = newIdempotencyKey(),
                status = SyncOutboxStatus.PENDING,
            ),
        )
    }

    suspend fun enqueueProgress(documentId: String, nowMs: Long = System.currentTimeMillis()) {
        syncOutboxDao.upsert(
            SyncOutboxEntity(
                id = outboxId(SyncEntityType.PROGRESS, documentId),
                entityType = SyncEntityType.PROGRESS,
                entityId = documentId,
                createdAtMs = nowMs,
                idempotencyKey = newIdempotencyKey(),
                status = SyncOutboxStatus.PENDING,
            ),
        )
    }

    suspend fun hasPendingMetadata(documentId: String): Boolean =
        syncOutboxDao.hasPending(SyncEntityType.METADATA, documentId)

    suspend fun hasPendingProgress(documentId: String): Boolean =
        syncOutboxDao.hasPending(SyncEntityType.PROGRESS, documentId)

    suspend fun clearEntity(entityType: SyncEntityType, entityId: String) {
        syncOutboxDao.deleteByEntity(entityType, entityId)
    }

    suspend fun clearAll() {
        syncOutboxDao.deleteAll()
    }

    private fun outboxId(entityType: SyncEntityType, entityId: String): String = "${entityType.name}:$entityId"

    private fun newIdempotencyKey(): String = UUID.randomUUID().toString()
}
