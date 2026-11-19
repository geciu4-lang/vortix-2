package com.vortix.app.feature.controls

import com.vortix.app.domain.model.AppUsageItem

data class EnergyControlsUiState(
    val isMonitoringEnabled: Boolean = true,
    val isChargeLimitEnabled: Boolean = true,
    val chargeTargetPercent: Int = 80,
    val isLowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThresholdPercent: Int = 20,
    val isThermalAlertEnabled: Boolean = true,
    val thermalThresholdCelsius: Int = 42,
    val isEcoGuardEnabled: Boolean = false,
    val designCapacityMah: Int = 5000,
    val hasUsageAccess: Boolean = false,
    val isLoadingUsage: Boolean = false,
    val topApps: List<AppUsageItem> = emptyList(),
    val message: String? = null
)
