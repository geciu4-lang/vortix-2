package com.vortix.app.feature.controls

import com.vortix.app.domain.model.AppUsageItem
import com.vortix.app.domain.model.DormantAppItem
import com.vortix.app.domain.model.EnergyChangeMarker
import com.vortix.app.domain.repository.EnergyMonitorScheduler
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.repository.UsageStatsRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnergyControlsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun enablingMonitoringSchedulesWorkerAndPersistsSetting() = runTest(dispatcher) {
        val settings = FakeSettings()
        val scheduler = FakeScheduler()
        val viewModel = EnergyControlsViewModel(settings, scheduler, FakeUsageStats())
        runCurrent()

        viewModel.setMonitoringEnabled(true)
        runCurrent()

        assertTrue(scheduler.started)
        assertTrue(settings.enabled.value)
        assertTrue(viewModel.uiState.value.isMonitoringEnabled)
    }

    @Test
    fun disablingMonitoringCancelsWorker() = runTest(dispatcher) {
        val settings = FakeSettings(initialEnabled = true)
        val scheduler = FakeScheduler()
        val viewModel = EnergyControlsViewModel(settings, scheduler, FakeUsageStats())
        runCurrent()

        viewModel.setMonitoringEnabled(false)
        runCurrent()

        assertTrue(scheduler.stopped)
        assertFalse(settings.enabled.value)
    }

    @Test
    fun chargeTargetIsClampedToSupportedRange() = runTest(dispatcher) {
        val settings = FakeSettings()
        val viewModel = EnergyControlsViewModel(settings, FakeScheduler(), FakeUsageStats())
        runCurrent()

        viewModel.setChargeTarget(120)
        runCurrent()

        assertEquals(100, settings.target.value)
    }

    @Test
    fun usageListIsLoadedOnlyWhenAccessIsGranted() = runTest(dispatcher) {
        val usage = FakeUsageStats(granted = true)
        val expected = listOf(AppUsageItem("com.example.app", "Example", 120_000L, 0L))
        usage.items = expected
        val viewModel = EnergyControlsViewModel(FakeSettings(), FakeScheduler(), usage)
        runCurrent()

        assertTrue(viewModel.uiState.value.hasUsageAccess)
        assertEquals(expected, viewModel.uiState.value.topApps)
    }

    private class FakeSettings(initialEnabled: Boolean = false) : EnergySettingsRepository {
        val enabled = MutableStateFlow(initialEnabled)
        val target = MutableStateFlow(80)
        val alertSent = MutableStateFlow(false)
        override val monitoringEnabled: Flow<Boolean> = enabled
        override val chargeTargetPercent: Flow<Int> = target
        override val chargeTargetAlertSent: Flow<Boolean> = alertSent
        override val thermalAlertSent: Flow<Boolean> = MutableStateFlow(false)
        override val designCapacityMah: Flow<Int> = MutableStateFlow(0)
        override val energyChangeMarkers: Flow<List<EnergyChangeMarker>> = MutableStateFlow(emptyList())
        override val batteryBaselineAtEpochMillis: Flow<Long> = MutableStateFlow(0L)
        override suspend fun setMonitoringEnabled(enabled: Boolean) { this.enabled.value = enabled }
        override suspend fun setChargeTargetPercent(percent: Int) { target.value = percent.coerceIn(70, 100) }
        override suspend fun setChargeTargetAlertSent(sent: Boolean) { alertSent.value = sent }
        override suspend fun setThermalAlertSent(sent: Boolean) = Unit
        override suspend fun setDesignCapacityMah(capacityMah: Int) = Unit
        override suspend fun addEnergyChangeMarker(marker: EnergyChangeMarker) = Unit
        override suspend fun setBatteryBaselineAtEpochMillis(epochMillis: Long) = Unit
    }

    private class FakeScheduler : EnergyMonitorScheduler {
        var started = false
        var stopped = false
        override fun start() { started = true; stopped = false }
        override fun stop() { stopped = true; started = false }
    }

    private class FakeUsageStats(
        private val granted: Boolean = false
    ) : UsageStatsRepository {
        var items: List<AppUsageItem> = emptyList()
        override fun hasUsageAccess(): Boolean = granted
        override suspend fun loadTodayUsage(): List<AppUsageItem> = items
        override suspend fun loadDormantApps(minimumUnusedDays: Int): List<DormantAppItem> = emptyList()
    }
}
