package io.mikoshift.natsu.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat

@Database(
    entities = [
        DocumentEntity::class,
        ReadingProgressEntity::class,
        DocumentCacheEntity::class,
        SyncOutboxEntity::class,
        SyncStateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(NatsuTypeConverters::class)
abstract class NatsuDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun documentCacheDao(): DocumentCacheDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun create(context: Context): NatsuDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NatsuDatabase::class.java,
                "natsu.db",
            )
                .addMigrations(*NatsuDatabaseMigrations.ALL)
                .build()
    }
}

class NatsuTypeConverters {
    @TypeConverter
    fun fromSourceFormat(value: SourceFormat): String = value.name

    @TypeConverter
    fun toSourceFormat(value: String): SourceFormat = SourceFormat.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)

    @TypeConverter
    fun fromSyncEntityType(value: SyncEntityType): String = value.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter
    fun fromSyncOutboxStatus(value: SyncOutboxStatus): String = value.name

    @TypeConverter
    fun toSyncOutboxStatus(value: String): SyncOutboxStatus = SyncOutboxStatus.valueOf(value)
}
