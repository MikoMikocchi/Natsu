package io.mikoshift.natsudroid.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.mikoshift.natsudroid.core.model.DocumentStatus
import io.mikoshift.natsudroid.core.model.SourceFormat

@Database(
    entities = [
        DocumentEntity::class,
        ReadingProgressEntity::class,
        DocumentCacheEntity::class,
        SyncOutboxEntity::class,
        SyncStateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(NatsudroidTypeConverters::class)
abstract class NatsudroidDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    abstract fun readingProgressDao(): ReadingProgressDao

    abstract fun documentCacheDao(): DocumentCacheDao

    abstract fun syncOutboxDao(): SyncOutboxDao

    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun create(context: Context): NatsudroidDatabase {
            val builder =
                Room.databaseBuilder(
                    context.applicationContext,
                    NatsudroidDatabase::class.java,
                    "natsudroid.db",
                )
            return NatsudroidDatabaseMigrations.ALL.fold(builder) { current, migration ->
                current.addMigrations(migration)
            }.build()
        }
    }
}

class NatsudroidTypeConverters {
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
