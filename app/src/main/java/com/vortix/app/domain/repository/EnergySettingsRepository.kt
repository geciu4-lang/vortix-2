package com.vortix.app.domain.repository

import kotlinx.coroutines.flow.Flow
import com.vortix.app.domain.model.EnergyChangeMarker

interface EnergySettingsRepository {
    val monitoringEnabled: Flow<Boolean>
    val chargeTargetPercent: Flow<Int>
    val chargeTargetAlertSent: Flow<Boolean>
    val thermalAlertSent: Flow<Boolean>
    val designCapacityMah: Flow<Int>
    val energyChangeMarkers: Flow<List<EnergyChangeMarker>>
    val batteryBaselineAtEpochMillis: Flow<Long>
    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setChargeTargetPercent(percent: Int)
    suspend fun setChargeTargetAlertSent(sent: Boolean)
    suspend fun setThermalAlertSent(sent: Boolean)
    suspend fun setDesignCapacityMah(capacityMah: Int)
    suspend fun addEnergyChangeMarker(marker: EnergyChangeMarker)
    suspend fun setBatteryBaselineAtEpochMillis(epochMillis: Long)
}
