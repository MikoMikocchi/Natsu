package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentSearchResult
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(): Flow<List<Document>>

    suspend fun sync(): Result<Unit>

    suspend fun search(query: String): Result<List<DocumentSearchResult>>

    suspend fun import(contentUri: String): Result<Document>

    suspend fun markDeleted(id: String): Result<Unit>

    suspend fun clearOnLogout()
}
