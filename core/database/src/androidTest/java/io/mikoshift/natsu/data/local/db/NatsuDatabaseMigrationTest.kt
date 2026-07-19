package io.mikoshift.natsu.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NatsuDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NatsuDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate2To3_preservesDocumentsProgressOutboxAndSyncCursor() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO documents (
                    id, title, sourceFormat, status, importError, importedAt, charCount,
                    updatedAtMs, packageSizeBytes, packageUpdatedAtMs, packageSha256, deleted
                ) VALUES (
                    'doc-1', 'Book', 'EPUB', 'READY', NULL, 0, 1000,
                    500, 0, 0, NULL, 0
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO reading_progress (
                    documentId, lastReadCharOffset, lastReadSectionId, lastReadBlockIndex,
                    lastReadBlockCharOffset, updatedAtMs, clientUpdatedAtMs
                ) VALUES (
                    'doc-1', 42, 'section-1', 3, 7, 500, 500
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sync_outbox (
                    id, entityType, entityId, createdAtMs, status, attempts, lastError
                ) VALUES (
                    'METADATA:doc-1', 'METADATA', 'doc-1', 500, 'PENDING', 0, NULL
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO sync_state (id, metadataSinceMs, progressSinceMs) VALUES (1, 100, 250)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            NatsuDatabaseMigrations.MIGRATION_2_3,
        )

        db.query("SELECT title FROM documents WHERE id = 'doc-1'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Book", cursor.getString(0))
        }

        db.query(
            "SELECT lastReadCharOffset FROM reading_progress WHERE documentId = 'doc-1'",
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(42, cursor.getInt(0))
        }

        db.query(
            "SELECT status FROM sync_outbox WHERE id = 'METADATA:doc-1'",
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("PENDING", cursor.getString(0))
        }

        db.query("SELECT documentsSinceMs FROM sync_state WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(250L, cursor.getLong(0))
        }

        db.close()
    }

    @Test
    fun migrate3To4_addsIdempotencyKeyToOutbox() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO sync_outbox (
                    id, entityType, entityId, createdAtMs, status, attempts, lastError
                ) VALUES (
                    'METADATA:doc-1', 'METADATA', 'doc-1', 500, 'PENDING', 0, NULL
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            NatsuDatabaseMigrations.MIGRATION_3_4,
        )

        db.query(
            "SELECT idempotencyKey FROM sync_outbox WHERE id = 'METADATA:doc-1'",
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("METADATA:doc-1:500", cursor.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate1To2_splitsEmbeddedDocumentFieldsIntoDedicatedTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `documents` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `sourceFormat` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `importError` TEXT,
                    `importedAt` INTEGER NOT NULL,
                    `charCount` INTEGER NOT NULL,
                    `lastReadCharOffset` INTEGER NOT NULL,
                    `lastReadSectionId` TEXT,
                    `lastReadBlockIndex` INTEGER NOT NULL,
                    `lastReadBlockCharOffset` INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL,
                    `packageSizeBytes` INTEGER NOT NULL,
                    `packageUpdatedAtMs` INTEGER NOT NULL,
                    `packageSha256` TEXT,
                    `deleted` INTEGER NOT NULL,
                    `isDirty` INTEGER NOT NULL,
                    `localPackagePath` TEXT,
                    `cachedPackageSha256` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_state` (
                    `id` INTEGER NOT NULL,
                    `lastSinceMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO documents (
                    id, title, sourceFormat, status, importError, importedAt, charCount,
                    lastReadCharOffset, lastReadSectionId, lastReadBlockIndex, lastReadBlockCharOffset,
                    updatedAtMs, packageSizeBytes, packageUpdatedAtMs, packageSha256, deleted,
                    isDirty, localPackagePath, cachedPackageSha256
                ) VALUES (
                    'doc-1', 'Legacy Book', 'EPUB', 'READY', NULL, 0, 1000,
                    15, 'chapter-2', 1, 2,
                    900, 1024, 900, 'sha', 0,
                    1, '/tmp/book.epub', 'cached-sha'
                )
                """.trimIndent(),
            )
            execSQL("INSERT INTO sync_state (id, lastSinceMs) VALUES (1, 777)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            NatsuDatabaseMigrations.MIGRATION_1_2,
        )

        db.query("SELECT title FROM documents WHERE id = 'doc-1'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Legacy Book", cursor.getString(0))
        }

        db.query(
            """
            SELECT lastReadCharOffset, lastReadSectionId
            FROM reading_progress
            WHERE documentId = 'doc-1'
            """.trimIndent(),
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(15, cursor.getInt(0))
            assertEquals("chapter-2", cursor.getString(1))
        }

        db.query(
            """
            SELECT localPackagePath, cachedPackageSha256
            FROM document_cache
            WHERE documentId = 'doc-1'
            """.trimIndent(),
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("/tmp/book.epub", cursor.getString(0))
            assertEquals("cached-sha", cursor.getString(1))
        }

        db.query(
            "SELECT entityType, status FROM sync_outbox WHERE entityId = 'doc-1'",
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("METADATA", cursor.getString(0))
            assertEquals("PENDING", cursor.getString(1))
        }

        db.query(
            "SELECT metadataSinceMs, progressSinceMs FROM sync_state WHERE id = 1",
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(777L, cursor.getLong(0))
            assertEquals(777L, cursor.getLong(1))
        }

        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
