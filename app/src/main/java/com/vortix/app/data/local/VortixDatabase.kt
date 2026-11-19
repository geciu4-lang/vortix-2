package com.vortix.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vortix.app.data.local.dao.BatterySampleDao
import com.vortix.app.data.local.entity.BatterySampleEntity

@Database(
    entities = [BatterySampleEntity::class],
    version = 2,
    exportSchema = true
)
abstract class VortixDatabase : RoomDatabase() {
    abstract fun batterySampleDao(): BatterySampleDao
}


/** Adds context fields without discarding previously collected battery history. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE battery_samples ADD COLUMN screenInteractive INTEGER")
        database.execSQL("ALTER TABLE battery_samples ADD COLUMN thermalStatus INTEGER")
        database.execSQL("ALTER TABLE battery_samples ADD COLUMN powerSaveMode INTEGER")
        database.execSQL("ALTER TABLE battery_samples ADD COLUMN deviceIdleMode INTEGER")
        database.execSQL("ALTER TABLE battery_samples ADD COLUMN dischargePredictionMillis INTEGER")
    }
}
