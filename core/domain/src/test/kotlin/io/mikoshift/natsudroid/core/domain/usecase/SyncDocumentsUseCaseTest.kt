package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.model.DocumentError
import io.mikoshift.natsudroid.core.model.SyncState
import io.mikoshift.natsudroid.core.testing.analytics.FakeAnalyticsTracker
import io.mikoshift.natsudroid.core.testing.fixture.AuthFixtures
import io.mikoshift.natsudroid.core.testing.repository.FakeAuthRepository
import io.mikoshift.natsudroid.core.testing.repository.FakeDocumentRepository
import io.mikoshift.natsudroid.core.testing.repository.FakeSyncStatusRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDocumentsUseCaseTest {
    @Test
    fun invoke_whenLoggedOut_skipsSync() = runTest {
        val documentRepository = FakeDocumentRepository()
        val syncStatusRepository = FakeSyncStatusRepository()
        val useCase =
            SyncDocumentsUseCase(
                authRepository = FakeAuthRepository(session = null),
                documentRepository = documentRepository,
                syncStatusRepository = syncStatusRepository,
                analyticsTracker = FakeAnalyticsTracker(),
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
        val useCase =
            SyncDocumentsUseCase(
                authRepository = FakeAuthRepository(session = AuthFixtures.session()),
                documentRepository = documentRepository,
                syncStatusRepository = syncStatusRepository,
                analyticsTracker = FakeAnalyticsTracker(),
            )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, documentRepository.syncCallCount)
        assertEquals(SyncState.Idle, syncStatusRepository.syncState.value)
    }

    @Test
    fun invoke_whenSyncFails_setsFailedStatusAndTracksAnalytics() = runTest {
        val documentRepository =
            FakeDocumentRepository(
                syncResult = Result.failure(DocumentError.NetworkFailure),
            )
        val syncStatusRepository = FakeSyncStatusRepository()
        val analyticsTracker = FakeAnalyticsTracker()
        val useCase =
            SyncDocumentsUseCase(
                authRepository = FakeAuthRepository(session = AuthFixtures.session()),
                documentRepository = documentRepository,
                syncStatusRepository = syncStatusRepository,
                analyticsTracker = analyticsTracker,
            )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(SyncState.Failed("Network error, please try again"), syncStatusRepository.syncState.value)
        assertEquals(listOf("sync_failed" to emptyMap<String, String>()), analyticsTracker.events)
    }
}
