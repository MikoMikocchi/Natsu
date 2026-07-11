package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastSinceMs: Long = 0,
)
