package io.mikoshift.natsudroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Transaction
    @Query(
        """
        SELECT d.* FROM documents d
        LEFT JOIN reading_progress rp ON d.id = rp.documentId
        WHERE d.deleted = 0
        ORDER BY COALESCE(rp.updatedAtMs, d.updatedAtMs) DESC
        """,
    )
    fun observeLibrary(): Flow<List<DocumentWithRelations>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getWithRelationsById(id: String): DocumentWithRelations?

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: String): Flow<DocumentWithRelations?>

    @Transaction
    @Query(
        """
        SELECT d.* FROM documents d
        LEFT JOIN document_cache dc ON d.id = dc.documentId
        WHERE d.deleted = 0 AND d.status = 'READY'
        AND d.packageSha256 IS NOT NULL
        AND (dc.cachedPackageSha256 IS NULL OR dc.cachedPackageSha256 != d.packageSha256)
        """,
    )
    suspend fun getDocumentsNeedingPackageDownload(): List<DocumentWithRelations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<DocumentEntity>)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}
