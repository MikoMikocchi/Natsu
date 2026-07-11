package io.mikoshift.natsu.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncStateDao {

    @Query("SELECT lastSinceMs FROM sync_state WHERE id = 1")
    suspend fun getLastSinceMs(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)
}
