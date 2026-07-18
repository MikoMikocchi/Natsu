package io.mikoshift.natsu.data.local

import io.mikoshift.natsu.data.local.db.SyncStateDao
import io.mikoshift.natsu.data.local.db.SyncStateEntity

class SyncCursorStore(private val syncStateDao: SyncStateDao) {

    suspend fun getDocumentsSinceMs(): Long = syncStateDao.get()?.documentsSinceMs ?: 0L

    suspend fun setDocumentsSinceMs(value: Long) {
        val current = syncStateDao.get() ?: SyncStateEntity()
        syncStateDao.upsert(current.copy(documentsSinceMs = value))
    }

    suspend fun clear() {
        syncStateDao.upsert(SyncStateEntity())
    }
}
