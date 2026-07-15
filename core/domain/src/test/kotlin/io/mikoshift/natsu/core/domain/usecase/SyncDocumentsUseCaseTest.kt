package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentSearchResult
import io.mikoshift.natsu.core.model.SyncState
import io.mikoshift.natsu.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDocumentsUseCaseTest {

    @Test
    fun invoke_whenLoggedOut_skipsSync() = runTest {
        val documentRepository = FakeDocumentRepository()
        val syncStatusRepository = FakeSyncStatusRepository()
        val useCase = SyncDocumentsUseCase(
            authRepository = FakeAuthRepository(session = null),
            documentRepository = documentRepository,
            syncStatusRepository = syncStatusRepository,
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, documentRepository.syncCallCount)
        assertEquals(SyncState.Idle, syncStatusRepository.syncState.value)
    }

    @Test
    fun invoke_whenLoggedIn_syncsAndUpdatesStatus() = runTest {
        val documentRepository = FakeDocumentRepository()
        val syncStatusRepository = FakeSyncStatusRepository()
        val useCase = SyncDocumentsUseCase(
            authRepository = FakeAuthRepository(session = sampleSession()),
            documentRepository = documentRepository,
            syncStatusRepository = syncStatusRepository,
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, documentRepository.syncCallCount)
        assertEquals(SyncState.Idle, syncStatusRepository.syncState.value)
    }

    @Test
    fun invoke_whenSyncFails_setsFailedStatus() = runTest {
        val documentRepository = FakeDocumentRepository(
            syncResult = Result.failure(DocumentError.NetworkFailure),
        )
        val syncStatusRepository = FakeSyncStatusRepository()
        val useCase = SyncDocumentsUseCase(
            authRepository = FakeAuthRepository(session = sampleSession()),
            documentRepository = documentRepository,
            syncStatusRepository = syncStatusRepository,
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(SyncState.Failed("Network error, please try again"), syncStatusRepository.syncState.value)
    }

    private fun sampleSession() = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        userId = 1L,
        userName = "Test User",
        userEmail = "test@example.com",
    )

    private class FakeAuthRepository(
        session: AuthSession?,
    ) : AuthRepository {
        override val isLoggedIn: Flow<Boolean> = emptyFlow()
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(session)

        override suspend fun register(
            name: String,
            email: String,
            password: String,
            passwordConfirmation: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override suspend fun getUser(): Result<User> =
            Result.failure(UnsupportedOperationException())

        override suspend fun forgotPassword(email: String): Result<String> =
            Result.failure(UnsupportedOperationException())

        override suspend fun resetPassword(
            token: String,
            password: String,
            passwordConfirmation: String,
        ): Result<String> = Result.failure(UnsupportedOperationException())

        override suspend fun changePassword(
            currentPassword: String,
            password: String,
            passwordConfirmation: String,
        ): Result<String> = Result.failure(UnsupportedOperationException())

        override suspend fun deleteAccount(password: String): Result<Unit> = Result.success(Unit)

        override suspend fun getSessions(): Result<List<DeviceSession>> =
            Result.failure(UnsupportedOperationException())

        override suspend fun revokeSession(id: Long, isCurrentSession: Boolean): Result<Unit> =
            Result.success(Unit)
    }

    private class FakeDocumentRepository(
        private val syncResult: Result<Unit> = Result.success(Unit),
    ) : DocumentRepository {
        var syncCallCount = 0

        override fun observeLibrary(): Flow<List<Document>> = emptyFlow()

        override suspend fun sync(): Result<Unit> {
            syncCallCount += 1
            return syncResult
        }

        override suspend fun search(query: String): Result<List<DocumentSearchResult>> =
            Result.success(emptyList())

        override suspend fun import(contentUri: String): Result<Document> =
            Result.failure(UnsupportedOperationException())

        override suspend fun markDeleted(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun clearOnLogout() = Unit
    }

    private class FakeSyncStatusRepository : SyncStatusRepository {
        private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        override val syncState: StateFlow<SyncState> = _syncState

        override fun setSyncing() {
            _syncState.value = SyncState.Syncing
        }

        override fun setIdle() {
            _syncState.value = SyncState.Idle
        }

        override fun setFailed(message: String?) {
            _syncState.value = SyncState.Failed(message)
        }
    }
}
