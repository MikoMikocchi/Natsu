package io.mikoshift.natsu.core.testing.repository

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDocumentRepository(
    documents: List<Document> = emptyList(),
    private val syncResult: Result<Unit> = Result.success(Unit),
) : DocumentRepository {
    private val library = MutableStateFlow(documents)
    var syncCallCount = 0
        private set

    override fun observeLibrary(): Flow<List<Document>> = library.asStateFlow()

    override suspend fun sync(): Result<Unit> {
        syncCallCount += 1
        return syncResult
    }

    override suspend fun search(query: String): Result<List<DocumentSearchResult>> =
        Result.success(emptyList())

    override suspend fun import(contentUri: String): Result<Document> =
        Result.failure(UnsupportedOperationException())

    override suspend fun markDeleted(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun clearOnLogout() {
        library.value = emptyList()
    }

    fun setDocuments(documents: List<Document>) {
        library.value = documents
    }
}
