package io.mikoshift.natsu.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.remote.dto.SourceFormat

@Database(
    entities = [DocumentEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DocumentTypeConverters::class)
abstract class NatsuDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun create(context: Context): NatsuDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NatsuDatabase::class.java,
                "natsu.db",
            ).build()
    }
}

class DocumentTypeConverters {
    @TypeConverter
    fun fromSourceFormat(value: SourceFormat): String = value.name

    @TypeConverter
    fun toSourceFormat(value: String): SourceFormat = SourceFormat.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)
}
