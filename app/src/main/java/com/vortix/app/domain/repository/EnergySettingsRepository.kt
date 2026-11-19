package com.vortix.app.domain.repository

import com.vortix.app.domain.model.EnergyChangeMarker
import kotlinx.coroutines.flow.Flow

interface EnergySettingsRepository {
    val monitoringEnabled: Flow<Boolean>
    val chargeLimitEnabled: Flow<Boolean>
    val chargeTargetPercent: Flow<Int>
    val chargeTargetAlertSent: Flow<Boolean>
    val lowBatteryAlertEnabled: Flow<Boolean>
    val lowBatteryThresholdPercent: Flow<Int>
    val thermalAlertEnabled: Flow<Boolean>
    val thermalThresholdCelsius: Flow<Int>
    val thermalAlertSent: Flow<Boolean>
    val ecoGuardEnabled: Flow<Boolean>
    val designCapacityMah: Flow<Int>
    val energyChangeMarkers: Flow<List<EnergyChangeMarker>>
    val batteryBaselineAtEpochMillis: Flow<Long>

    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setChargeLimitEnabled(enabled: Boolean)
    suspend fun setChargeTargetPercent(percent: Int)
    suspend fun setChargeTargetAlertSent(sent: Boolean)
    suspend fun setLowBatteryAlertEnabled(enabled: Boolean)
    suspend fun setLowBatteryThresholdPercent(percent: Int)
    suspend fun setThermalAlertEnabled(enabled: Boolean)
    suspend fun setThermalThresholdCelsius(celsius: Int)
    suspend fun setThermalAlertSent(sent: Boolean)
    suspend fun setEcoGuardEnabled(enabled: Boolean)
    suspend fun setDesignCapacityMah(capacityMah: Int)
    suspend fun addEnergyChangeMarker(marker: EnergyChangeMarker)
    suspend fun setBatteryBaselineAtEpochMillis(epochMillis: Long)
}
