package io.mikoshift.natsu.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE deleted = 0 ORDER BY updatedAtMs DESC")
    fun observeLibrary(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDirty = 1")
    suspend fun getDirtyDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query(
        """
        SELECT * FROM documents
        WHERE deleted = 0 AND status = 'READY'
        AND packageSha256 IS NOT NULL
        AND (cachedPackageSha256 IS NULL OR cachedPackageSha256 != packageSha256)
        """,
    )
    suspend fun getDocumentsNeedingPackageDownload(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<DocumentEntity>)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}
