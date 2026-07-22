package io.mikoshift.natsudroid.data.local.db

import androidx.room.Embedded
import androidx.room.Relation

data class DocumentWithRelations(
    @Embedded val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId",
    )
    val progress: ReadingProgressEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId",
    )
    val cache: DocumentCacheEntity?,
)
