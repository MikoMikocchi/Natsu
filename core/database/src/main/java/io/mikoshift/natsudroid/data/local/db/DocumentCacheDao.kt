package io.mikoshift.natsudroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DocumentCacheDao {
    @Query("SELECT * FROM document_cache WHERE documentId = :documentId")
    suspend fun getByDocumentId(documentId: String): DocumentCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: DocumentCacheEntity)

    @Query("DELETE FROM document_cache WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    @Query("DELETE FROM document_cache")
    suspend fun deleteAll()
}
