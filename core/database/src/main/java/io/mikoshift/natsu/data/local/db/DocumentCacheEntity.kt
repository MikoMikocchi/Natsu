package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_cache",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["documentId"], unique = true)],
)
data class DocumentCacheEntity(
    @PrimaryKey val documentId: String,
    val localPackagePath: String? = null,
    val cachedPackageSha256: String? = null,
)
