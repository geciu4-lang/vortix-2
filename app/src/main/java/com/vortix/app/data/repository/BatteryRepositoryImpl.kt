package com.vortix.app.data.repository

import com.vortix.app.data.battery.BatteryDataSource
import com.vortix.app.data.battery.mapper.toDomain
import com.vortix.app.data.battery.mapper.toEntity
import com.vortix.app.data.local.dao.BatterySampleDao
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.repository.BatteryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val dataSource: BatteryDataSource,
    private val batterySampleDao: BatterySampleDao
) : BatteryRepository {

    override fun observeLatestSnapshot(): Flow<BatterySnapshot?> =
        batterySampleDao.observeLatest().map { it?.toDomain() }

    override fun observeHistory(sinceEpochMillis: Long): Flow<List<BatterySnapshot>> =
        batterySampleDao.observeSince(sinceEpochMillis).map { rows -> rows.map { it.toDomain() } }

    override suspend fun loadHistory(sinceEpochMillis: Long): List<BatterySnapshot> =
        withContext(Dispatchers.IO) { batterySampleDao.loadSince(sinceEpochMillis).map { it.toDomain() } }

    override suspend fun pruneHistory(olderThanEpochMillis: Long): Int =
        withContext(Dispatchers.IO) { batterySampleDao.deleteBefore(olderThanEpochMillis) }

    override suspend fun refreshSnapshot(): BatterySnapshot = withContext(Dispatchers.IO) {
        val snapshot = dataSource.readSnapshot()
        batterySampleDao.insert(snapshot.toEntity())
        snapshot
    }
}
