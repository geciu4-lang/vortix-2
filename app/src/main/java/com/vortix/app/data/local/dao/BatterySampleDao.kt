package com.vortix.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vortix.app.data.local.entity.BatterySampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: BatterySampleEntity): Long

    @Query("SELECT * FROM battery_samples ORDER BY capturedAtEpochMillis DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<BatterySampleEntity?>

    @Query("SELECT * FROM battery_samples WHERE capturedAtEpochMillis >= :sinceEpochMillis ORDER BY capturedAtEpochMillis ASC, id ASC")
    fun observeSince(sinceEpochMillis: Long): Flow<List<BatterySampleEntity>>

    @Query("SELECT * FROM battery_samples WHERE capturedAtEpochMillis >= :sinceEpochMillis ORDER BY capturedAtEpochMillis ASC, id ASC")
    suspend fun loadSince(sinceEpochMillis: Long): List<BatterySampleEntity>

    @Query("DELETE FROM battery_samples WHERE capturedAtEpochMillis < :cutoffEpochMillis")
    suspend fun deleteBefore(cutoffEpochMillis: Long): Int

    @Query("SELECT COUNT(*) FROM battery_samples")
    suspend fun countSamples(): Int
}
