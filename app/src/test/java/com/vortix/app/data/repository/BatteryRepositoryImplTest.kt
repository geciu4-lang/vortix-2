package com.vortix.app.data.repository

import com.vortix.app.data.battery.BatteryDataSource
import com.vortix.app.data.local.dao.BatterySampleDao
import com.vortix.app.data.local.entity.BatterySampleEntity
import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BatteryRepositoryImplTest {
    @Test
    fun refreshReadsAndPersistsDeviceSnapshot() = runBlocking {
        val expected = BatterySnapshot(
            capturedAtEpochMillis = 100L,
            levelPercent = 64,
            status = BatteryStatus.DISCHARGING,
            pluggedSource = PluggedSource.NONE,
            temperatureCelsius = 29.4,
            voltageMillivolts = 3_980,
            chargeCounterMicroAh = 1_900_000,
            currentNowMicroAmps = -320_000,
            cycleCount = null,
            health = BatteryHealth.GOOD
        )
        val dataSource = FakeBatteryDataSource(expected)
        val dao = FakeBatterySampleDao()
        val repository = BatteryRepositoryImpl(dataSource, dao)

        val actual = repository.refreshSnapshot()

        assertEquals(expected, actual)
        assertNotNull(dao.latest.value)
        assertEquals(expected.capturedAtEpochMillis, dao.latest.value?.capturedAtEpochMillis)
        assertEquals(expected.currentNowMicroAmps, dao.latest.value?.currentNowMicroAmps)
    }

    private class FakeBatteryDataSource(
        private val snapshot: BatterySnapshot
    ) : BatteryDataSource {
        override fun readSnapshot(): BatterySnapshot = snapshot
    }

    private class FakeBatterySampleDao : BatterySampleDao {
        val latest = MutableStateFlow<BatterySampleEntity?>(null)

        override suspend fun insert(sample: BatterySampleEntity): Long {
            latest.value = sample
            return 1L
        }

        override fun observeLatest(): Flow<BatterySampleEntity?> = latest
        override fun observeSince(sinceEpochMillis: Long): Flow<List<BatterySampleEntity>> = MutableStateFlow(
            latest.value?.takeIf { it.capturedAtEpochMillis >= sinceEpochMillis }?.let(::listOf).orEmpty()
        )
        override suspend fun loadSince(sinceEpochMillis: Long): List<BatterySampleEntity> =
            latest.value?.takeIf { it.capturedAtEpochMillis >= sinceEpochMillis }?.let(::listOf).orEmpty()
        override suspend fun deleteBefore(cutoffEpochMillis: Long): Int = 0
        override suspend fun countSamples(): Int = if (latest.value == null) 0 else 1
    }
}
