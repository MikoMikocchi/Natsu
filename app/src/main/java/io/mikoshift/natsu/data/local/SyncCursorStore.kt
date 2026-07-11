package io.mikoshift.natsu.data.local

import io.mikoshift.natsu.data.local.db.SyncStateDao
import io.mikoshift.natsu.data.local.db.SyncStateEntity

class SyncCursorStore(private val syncStateDao: SyncStateDao) {

    suspend fun getLastSinceMs(): Long = syncStateDao.getLastSinceMs() ?: 0L

    suspend fun setLastSinceMs(value: Long) {
        syncStateDao.upsert(SyncStateEntity(lastSinceMs = value))
    }

    suspend fun clear() {
        syncStateDao.upsert(SyncStateEntity(lastSinceMs = 0L))
    }
}
