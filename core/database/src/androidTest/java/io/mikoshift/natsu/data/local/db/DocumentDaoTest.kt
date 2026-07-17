package io.mikoshift.natsu.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDaoTest {

    private lateinit var database: NatsuDatabase
    private lateinit var documentDao: DocumentDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NatsuDatabase::class.java,
        ).build()
        documentDao = database.documentDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun upsert_andObserveLibrary_returnsDocument() = runTest {
        val entity = DocumentEntity(
            id = "doc-1",
            title = "Test Book",
            sourceFormat = SourceFormat.EPUB,
            status = DocumentStatus.READY,
            updatedAtMs = 100L,
        )
        documentDao.upsert(entity)

        val documents = documentDao.observeLibrary().first()
        assertEquals(1, documents.size)
        assertEquals("Test Book", documents.first().title)
    }
}
