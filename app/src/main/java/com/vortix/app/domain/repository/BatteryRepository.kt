package com.vortix.app.domain.repository

import com.vortix.app.domain.model.BatterySnapshot
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observeLatestSnapshot(): Flow<BatterySnapshot?>
    suspend fun refreshSnapshot(): BatterySnapshot
    fun observeHistory(sinceEpochMillis: Long): Flow<List<BatterySnapshot>>
    suspend fun loadHistory(sinceEpochMillis: Long): List<BatterySnapshot>
    suspend fun pruneHistory(olderThanEpochMillis: Long): Int
}
