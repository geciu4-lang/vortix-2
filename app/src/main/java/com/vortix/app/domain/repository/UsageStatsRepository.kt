package com.vortix.app.domain.repository

import com.vortix.app.domain.model.AppUsageItem
import com.vortix.app.domain.model.DormantAppItem

interface UsageStatsRepository {
    fun hasUsageAccess(): Boolean
    suspend fun loadTodayUsage(): List<AppUsageItem>
    suspend fun loadDormantApps(minimumUnusedDays: Int = 14): List<DormantAppItem>
}
