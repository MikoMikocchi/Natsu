package io.mikoshift.natsudroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE documentId = :documentId")
    suspend fun getByDocumentId(documentId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<ReadingProgressEntity>)

    @Query("DELETE FROM reading_progress WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    @Query("DELETE FROM reading_progress")
    suspend fun deleteAll()
}
