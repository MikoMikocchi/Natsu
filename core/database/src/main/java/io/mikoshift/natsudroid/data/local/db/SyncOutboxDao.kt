package io.mikoshift.natsudroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE status IN ('PENDING', 'FAILED')
        ORDER BY createdAtMs ASC
        """,
    )
    suspend fun getPending(): List<SyncOutboxEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM sync_outbox
            WHERE entityType = :entityType
            AND entityId = :entityId
            AND status IN ('PENDING', 'FAILED', 'IN_PROGRESS')
        )
        """,
    )
    suspend fun hasPending(entityType: SyncEntityType, entityId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :status, attempts = :attempts, lastError = :lastError WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncOutboxStatus, attempts: Int, lastError: String?)

    @Query("UPDATE sync_outbox SET status = 'PENDING', lastError = NULL WHERE status = 'IN_PROGRESS'")
    suspend fun resetInProgressToPending()

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: SyncEntityType, entityId: String)

    @Query("DELETE FROM sync_outbox WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAll()
}
