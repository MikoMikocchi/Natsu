package io.mikoshift.natsu.data.local

import io.mikoshift.natsu.data.local.db.SyncStateDao
import io.mikoshift.natsu.data.local.db.SyncStateEntity

class SyncCursorStore(private val syncStateDao: SyncStateDao) {

    suspend fun getMetadataSinceMs(): Long = syncStateDao.get()?.metadataSinceMs ?: 0L

    suspend fun getProgressSinceMs(): Long = syncStateDao.get()?.progressSinceMs ?: 0L

    suspend fun setMetadataSinceMs(value: Long) {
        val current = syncStateDao.get() ?: SyncStateEntity()
        syncStateDao.upsert(current.copy(metadataSinceMs = value))
    }

    suspend fun setProgressSinceMs(value: Long) {
        val current = syncStateDao.get() ?: SyncStateEntity()
        syncStateDao.upsert(current.copy(progressSinceMs = value))
    }

    suspend fun clear() {
        syncStateDao.upsert(SyncStateEntity())
    }
}
