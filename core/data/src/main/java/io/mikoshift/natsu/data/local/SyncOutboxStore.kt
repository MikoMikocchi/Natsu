package io.mikoshift.natsu.data.local

import io.mikoshift.natsu.data.local.db.SyncEntityType
import io.mikoshift.natsu.data.local.db.SyncOutboxDao
import io.mikoshift.natsu.data.local.db.SyncOutboxEntity
import io.mikoshift.natsu.data.local.db.SyncOutboxStatus

class SyncOutboxStore(
    private val syncOutboxDao: SyncOutboxDao,
) {

    suspend fun enqueueMetadata(documentId: String, nowMs: Long = System.currentTimeMillis()) {
        syncOutboxDao.upsert(
            SyncOutboxEntity(
                id = outboxId(SyncEntityType.METADATA, documentId),
                entityType = SyncEntityType.METADATA,
                entityId = documentId,
                createdAtMs = nowMs,
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

    private fun outboxId(entityType: SyncEntityType, entityId: String): String =
        "${entityType.name}:$entityId"
}
