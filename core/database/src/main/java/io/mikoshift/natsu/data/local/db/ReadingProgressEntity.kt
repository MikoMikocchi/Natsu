package io.mikoshift.natsu.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["updatedAtMs"]), Index(value = ["documentId"], unique = true)],
)
data class ReadingProgressEntity(
    @PrimaryKey val documentId: String,
    val lastReadCharOffset: Int = 0,
    val lastReadSectionId: String? = null,
    val lastReadBlockIndex: Int = 0,
    val lastReadBlockCharOffset: Int = 0,
    val updatedAtMs: Long = 0,
    val clientUpdatedAtMs: Long = 0,
)
