package io.mikoshift.natsu.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NatsuDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reading_progress` (
                    `documentId` TEXT NOT NULL,
                    `lastReadCharOffset` INTEGER NOT NULL,
                    `lastReadSectionId` TEXT,
                    `lastReadBlockIndex` INTEGER NOT NULL,
                    `lastReadBlockCharOffset` INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL,
                    `clientUpdatedAtMs` INTEGER NOT NULL,
                    PRIMARY KEY(`documentId`),
                    FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reading_progress_updatedAtMs` " +
                    "ON `reading_progress` (`updatedAtMs`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_progress_documentId` " +
                    "ON `reading_progress` (`documentId`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `document_cache` (
                    `documentId` TEXT NOT NULL,
                    `localPackagePath` TEXT,
                    `cachedPackageSha256` TEXT,
                    PRIMARY KEY(`documentId`),
                    FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_document_cache_documentId` " +
                    "ON `document_cache` (`documentId`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_outbox` (
                    `id` TEXT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `createdAtMs` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `attempts` INTEGER NOT NULL,
                    `lastError` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_sync_outbox_status_createdAtMs` " +
                    "ON `sync_outbox` (`status`, `createdAtMs`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_outbox_entityType_entityId` " +
                    "ON `sync_outbox` (`entityType`, `entityId`)",
            )

            db.execSQL(
                """
                INSERT INTO `reading_progress` (
                    `documentId`,
                    `lastReadCharOffset`,
                    `lastReadSectionId`,
                    `lastReadBlockIndex`,
                    `lastReadBlockCharOffset`,
                    `updatedAtMs`,
                    `clientUpdatedAtMs`
                )
                SELECT
                    `id`,
                    `lastReadCharOffset`,
                    `lastReadSectionId`,
                    `lastReadBlockIndex`,
                    `lastReadBlockCharOffset`,
                    `updatedAtMs`,
                    `updatedAtMs`
                FROM `documents`
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `document_cache` (`documentId`, `localPackagePath`, `cachedPackageSha256`)
                SELECT `id`, `localPackagePath`, `cachedPackageSha256`
                FROM `documents`
                WHERE `localPackagePath` IS NOT NULL OR `cachedPackageSha256` IS NOT NULL
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `sync_outbox` (
                    `id`,
                    `entityType`,
                    `entityId`,
                    `createdAtMs`,
                    `status`,
                    `attempts`,
                    `lastError`
                )
                SELECT
                    'METADATA:' || `id`,
                    'METADATA',
                    `id`,
                    `updatedAtMs`,
                    'PENDING',
                    0,
                    NULL
                FROM `documents`
                WHERE `isDirty` = 1
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `documents_new` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `sourceFormat` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `importError` TEXT,
                    `importedAt` INTEGER NOT NULL,
                    `charCount` INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL,
                    `packageSizeBytes` INTEGER NOT NULL,
                    `packageUpdatedAtMs` INTEGER NOT NULL,
                    `packageSha256` TEXT,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `documents_new` (
                    `id`,
                    `title`,
                    `sourceFormat`,
                    `status`,
                    `importError`,
                    `importedAt`,
                    `charCount`,
                    `updatedAtMs`,
                    `packageSizeBytes`,
                    `packageUpdatedAtMs`,
                    `packageSha256`,
                    `deleted`
                )
                SELECT
                    `id`,
                    `title`,
                    `sourceFormat`,
                    `status`,
                    `importError`,
                    `importedAt`,
                    `charCount`,
                    `updatedAtMs`,
                    `packageSizeBytes`,
                    `packageUpdatedAtMs`,
                    `packageSha256`,
                    `deleted`
                FROM `documents`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `documents`")
            db.execSQL("ALTER TABLE `documents_new` RENAME TO `documents`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_documents_updatedAtMs` ON `documents` (`updatedAtMs`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_state_new` (
                    `id` INTEGER NOT NULL,
                    `metadataSinceMs` INTEGER NOT NULL,
                    `progressSinceMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `sync_state_new` (`id`, `metadataSinceMs`, `progressSinceMs`)
                SELECT `id`, `lastSinceMs`, `lastSinceMs`
                FROM `sync_state`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `sync_state`")
            db.execSQL("ALTER TABLE `sync_state_new` RENAME TO `sync_state`")

            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_state_new` (
                    `id` INTEGER NOT NULL,
                    `documentsSinceMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `sync_state_new` (`id`, `documentsSinceMs`)
                SELECT
                    `id`,
                    CASE
                        WHEN `metadataSinceMs` >= `progressSinceMs` THEN `metadataSinceMs`
                        ELSE `progressSinceMs`
                    END
                FROM `sync_state`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `sync_state`")
            db.execSQL("ALTER TABLE `sync_state_new` RENAME TO `sync_state`")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE `sync_outbox`
                ADD COLUMN `idempotencyKey` TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
            db.execSQL(
                """
                UPDATE `sync_outbox`
                SET `idempotencyKey` = `id` || ':' || CAST(`createdAtMs` AS TEXT)
                WHERE `idempotencyKey` = ''
                """.trimIndent(),
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
