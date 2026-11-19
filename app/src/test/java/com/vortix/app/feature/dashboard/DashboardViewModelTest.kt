package com.vortix.app.feature.dashboard

import android.content.Context
import com.vortix.app.data.battery.SystemDesignCapacityReader
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.EnergyChangeMarker
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.domain.repository.BatteryRepository
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.usecase.ObserveLatestBatterySnapshotUseCase
import com.vortix.app.domain.usecase.ObserveRecentBatterySnapshotsUseCase
import com.vortix.app.domain.usecase.RefreshBatterySnapshotUseCase
import java.lang.reflect.Proxy
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

    private fun createViewModel(repository: BatteryRepository): DashboardViewModel {
        val settingsRepository = FakeSettingsRepository()
        val fakeContext = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "FakeContext"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    Float::class.javaPrimitiveType -> 0f
                    Double::class.javaPrimitiveType -> 0.0
                    else -> null
                }
            }
        } as Context
        return DashboardViewModel(
            observeLatestSnapshot = ObserveLatestBatterySnapshotUseCase(repository),
            observeRecentSnapshots = ObserveRecentBatterySnapshotsUseCase(repository),
            refreshSnapshot = RefreshBatterySnapshotUseCase(repository),
            batteryRepository = repository,
            settingsRepository = settingsRepository,
            systemCapacityReader = SystemDesignCapacityReader(fakeContext)
        )
    }

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

    private class FakeSettingsRepository : EnergySettingsRepository {
        private val cap = MutableStateFlow(5000)
        override val monitoringEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val chargeLimitEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val chargeTargetPercent: Flow<Int> = MutableStateFlow(80)
        override val chargeTargetAlertSent: Flow<Boolean> = MutableStateFlow(false)
        override val lowBatteryAlertEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val lowBatteryThresholdPercent: Flow<Int> = MutableStateFlow(20)
        override val thermalAlertEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val thermalThresholdCelsius: Flow<Int> = MutableStateFlow(42)
        override val thermalAlertSent: Flow<Boolean> = MutableStateFlow(false)
        override val ecoGuardEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val designCapacityMah: Flow<Int> = cap
        override val energyChangeMarkers: Flow<List<EnergyChangeMarker>> = MutableStateFlow(emptyList())
        override val batteryBaselineAtEpochMillis: Flow<Long> = MutableStateFlow(0L)

        override suspend fun setMonitoringEnabled(enabled: Boolean) = Unit
        override suspend fun setChargeLimitEnabled(enabled: Boolean) = Unit
        override suspend fun setChargeTargetPercent(percent: Int) = Unit
        override suspend fun setChargeTargetAlertSent(sent: Boolean) = Unit
        override suspend fun setLowBatteryAlertEnabled(enabled: Boolean) = Unit
        override suspend fun setLowBatteryThresholdPercent(percent: Int) = Unit
        override suspend fun setThermalAlertEnabled(enabled: Boolean) = Unit
        override suspend fun setThermalThresholdCelsius(celsius: Int) = Unit
        override suspend fun setThermalAlertSent(sent: Boolean) = Unit
        override suspend fun setEcoGuardEnabled(enabled: Boolean) = Unit
        override suspend fun setDesignCapacityMah(capacityMah: Int) { cap.value = capacityMah }
        override suspend fun addEnergyChangeMarker(marker: EnergyChangeMarker) = Unit
        override suspend fun setBatteryBaselineAtEpochMillis(epochMillis: Long) = Unit
    }

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
