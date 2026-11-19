package com.vortix.app.feature.dashboard

import com.vortix.app.domain.model.BatterySnapshot

data class DashboardUiState(
    val snapshot: BatterySnapshot? = null,
    val recentSnapshots: List<BatterySnapshot> = emptyList(),
    val designCapacityMah: Int = 5000,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
