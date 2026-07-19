package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncEntityType {
    METADATA,
    PROGRESS,
}

enum class SyncOutboxStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CONFLICT,
}

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status", "createdAtMs"]),
        Index(value = ["entityType", "entityId"], unique = true),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val createdAtMs: Long,
    val idempotencyKey: String,
    val status: SyncOutboxStatus = SyncOutboxStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
)
