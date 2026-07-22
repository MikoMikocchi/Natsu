package io.mikoshift.natsudroid.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mikoshift.natsudroid.core.model.DocumentStatus
import io.mikoshift.natsudroid.core.model.SourceFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDaoTest {
    private lateinit var database: NatsudroidDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var readingProgressDao: ReadingProgressDao

    @Before
    fun createDb() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    NatsudroidDatabase::class.java,
                ).build()
        documentDao = database.documentDao()
        readingProgressDao = database.readingProgressDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun upsert_andObserveLibrary_returnsDocumentWithProgress() = runTest {
        val entity =
            DocumentEntity(
                id = "doc-1",
                title = "Test Book",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 100L,
            )
        documentDao.upsert(entity)
        readingProgressDao.upsert(
            ReadingProgressEntity(
                documentId = "doc-1",
                lastReadCharOffset = 42,
                updatedAtMs = 500L,
            ),
        )

        val documents = documentDao.observeLibrary().first()
        assertEquals(1, documents.size)
        assertEquals("Test Book", documents.first().document.title)
        assertNotNull(documents.first().progress)
        assertEquals(42, documents.first().progress?.lastReadCharOffset)
    }
}
