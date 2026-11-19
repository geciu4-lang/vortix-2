package com.vortix.app.feature.dashboard

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.domain.repository.BatteryRepository
import com.vortix.app.domain.usecase.ObserveLatestBatterySnapshotUseCase
import com.vortix.app.domain.usecase.RefreshBatterySnapshotUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialRefreshPublishesSnapshotAndStopsLoading() = runTest(mainDispatcher) {
        val expected = sampleSnapshot()
        val repository = FakeBatteryRepository(expected)
        val viewModel = createViewModel(repository)

        runCurrent()

        assertEquals(expected, viewModel.uiState.value.snapshot)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun refreshFailureShowsErrorAndStopsLoading() = runTest(mainDispatcher) {
        val repository = FakeBatteryRepository(failure = IllegalStateException("sensor unavailable"))
        val viewModel = createViewModel(repository)

        runCurrent()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private fun createViewModel(repository: BatteryRepository) = DashboardViewModel(
        observeLatestSnapshot = ObserveLatestBatterySnapshotUseCase(repository),
        refreshSnapshot = RefreshBatterySnapshotUseCase(repository)
    )

    private fun sampleSnapshot() = BatterySnapshot(
        capturedAtEpochMillis = 1_800_000_000_000L,
        levelPercent = 72,
        status = BatteryStatus.DISCHARGING,
        pluggedSource = PluggedSource.NONE,
        temperatureCelsius = 30.5,
        voltageMillivolts = 4_050,
        chargeCounterMicroAh = 2_100_000L,
        currentNowMicroAmps = -450_000L,
        cycleCount = null,
        health = null
    )

    private class FakeBatteryRepository(
        private val result: BatterySnapshot? = null,
        private val failure: Throwable? = null
    ) : BatteryRepository {
        private val latest = MutableStateFlow<BatterySnapshot?>(null)

        override fun observeLatestSnapshot(): Flow<BatterySnapshot?> = latest

        override suspend fun refreshSnapshot(): BatterySnapshot {
            failure?.let { throw it }
            val snapshot = requireNotNull(result)
            latest.value = snapshot
            return snapshot
        }
        override fun observeHistory(sinceEpochMillis: Long): Flow<List<BatterySnapshot>> =
            MutableStateFlow(emptyList())
        override suspend fun loadHistory(sinceEpochMillis: Long): List<BatterySnapshot> = emptyList()
        override suspend fun pruneHistory(olderThanEpochMillis: Long): Int = 0
    }
}
