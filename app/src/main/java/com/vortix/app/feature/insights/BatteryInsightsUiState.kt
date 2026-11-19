package com.vortix.app.feature.insights

import com.vortix.app.domain.model.DormantAppItem
import com.vortix.app.domain.model.NetworkUsageSnapshot
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.EnergyChangeMarker
import com.vortix.app.domain.model.DrainComparison
import com.vortix.app.domain.model.PowerInsightSummary

 data class BatteryInsightsUiState(
    val summary: PowerInsightSummary? = null,
    val history: List<BatterySnapshot> = emptyList(),
    val designCapacityMah: Int = 0,
    val batteryBaselineAtEpochMillis: Long = 0L,
    val networkUsage: NetworkUsageSnapshot? = null,
    val dormantApps: List<DormantAppItem> = emptyList(),
    val changeMarkers: List<EnergyChangeMarker> = emptyList(),
    val changeComparisons: Map<Long, DrainComparison> = emptyMap(),
    val isLoadingNetwork: Boolean = false,
    val isLoadingDormantApps: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val errorMessage: String? = null,
    val networkErrorMessage: String? = null,
    val dormantAppsErrorMessage: String? = null
)
