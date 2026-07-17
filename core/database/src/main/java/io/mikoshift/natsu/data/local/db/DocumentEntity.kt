package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat

@Entity(
    tableName = "documents",
    indices = [Index(value = ["updatedAtMs"])],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceFormat: SourceFormat,
    val status: DocumentStatus,
    val importError: String? = null,
    val importedAt: Long = 0,
    val charCount: Int = 0,
    val lastReadCharOffset: Int = 0,
    val lastReadSectionId: String? = null,
    val lastReadBlockIndex: Int = 0,
    val lastReadBlockCharOffset: Int = 0,
    val updatedAtMs: Long = 0,
    val packageSizeBytes: Long = 0,
    val packageUpdatedAtMs: Long = 0,
    val packageSha256: String? = null,
    val deleted: Boolean = false,
    val isDirty: Boolean = false,
    val localPackagePath: String? = null,
    val cachedPackageSha256: String? = null,
)
