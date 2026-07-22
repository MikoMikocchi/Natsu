package io.mikoshift.natsudroid.core.domain.repository

import io.mikoshift.natsudroid.core.model.Document
import io.mikoshift.natsudroid.core.model.DocumentSearchResult
import io.mikoshift.natsudroid.core.model.content.ReadingPosition
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(): Flow<List<Document>>

    fun observeDocument(id: String): Flow<Document?>

    suspend fun sync(): Result<Unit>

    suspend fun search(query: String): Result<List<DocumentSearchResult>>

    suspend fun import(contentUri: String): Result<Document>

    suspend fun markDeleted(id: String): Result<Unit>

    suspend fun ensurePackageDownloaded(id: String): Result<Unit>

    suspend fun updateReadingProgress(id: String, position: ReadingPosition): Result<Unit>

    suspend fun clearOnLogout()
}
