package io.mikoshift.natsu.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncOutboxDaoTest {

    private lateinit var database: NatsuDatabase
    private lateinit var syncOutboxDao: SyncOutboxDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NatsuDatabase::class.java,
        ).build()
        syncOutboxDao = database.syncOutboxDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getPending_includesPendingAndFailedStatuses() = runTest {
        syncOutboxDao.upsert(sampleEntry(id = "pending", status = SyncOutboxStatus.PENDING))
        syncOutboxDao.upsert(sampleEntry(id = "failed", status = SyncOutboxStatus.FAILED))
        syncOutboxDao.upsert(sampleEntry(id = "completed", status = SyncOutboxStatus.COMPLETED))

        val pending = syncOutboxDao.getPending()

        assertEquals(2, pending.size)
        assertTrue(pending.any { it.id == "pending" })
        assertTrue(pending.any { it.id == "failed" })
    }

    @Test
    fun resetInProgressToPending_makesStaleEntriesRetryable() = runTest {
        syncOutboxDao.upsert(
            sampleEntry(
                id = "stale",
                status = SyncOutboxStatus.IN_PROGRESS,
                attempts = 2,
                lastError = "timeout",
            ),
        )
        syncOutboxDao.upsert(sampleEntry(id = "pending", status = SyncOutboxStatus.PENDING))

        syncOutboxDao.resetInProgressToPending()

        val pending = syncOutboxDao.getPending()
        val stale = pending.single { it.id == "stale" }
        assertEquals(SyncOutboxStatus.PENDING, stale.status)
        assertEquals(null, stale.lastError)
        assertEquals(2, stale.attempts)
    }

    private fun sampleEntry(
        id: String,
        status: SyncOutboxStatus,
        attempts: Int = 0,
        lastError: String? = null,
    ) = SyncOutboxEntity(
        id = id,
        entityType = SyncEntityType.METADATA,
        entityId = "doc-$id",
        createdAtMs = 1L,
        idempotencyKey = "key-$id",
        status = status,
        attempts = attempts,
        lastError = lastError,
    )
}
