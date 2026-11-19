package com.vortix.app.domain.usecase

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshBatterySnapshotUseCaseTest {
    @Test
    fun delegatesRefreshToRepository() = runBlocking {
        val expected = BatterySnapshot(
            capturedAtEpochMillis = 9L,
            levelPercent = 50,
            status = BatteryStatus.FULL,
            pluggedSource = PluggedSource.AC,
            temperatureCelsius = null,
            voltageMillivolts = null,
            chargeCounterMicroAh = null,
            currentNowMicroAmps = null,
            cycleCount = null,
            health = null
        )
        val useCase = RefreshBatterySnapshotUseCase(
            object : BatteryRepository {
                override fun observeLatestSnapshot(): Flow<BatterySnapshot?> = emptyFlow()
                override suspend fun refreshSnapshot(): BatterySnapshot = expected
                override fun observeHistory(sinceEpochMillis: Long): Flow<List<BatterySnapshot>> = emptyFlow()
                override suspend fun loadHistory(sinceEpochMillis: Long): List<BatterySnapshot> = emptyList()
                override suspend fun pruneHistory(olderThanEpochMillis: Long): Int = 0
            }
        )

        assertEquals(expected, useCase())
    }
}
