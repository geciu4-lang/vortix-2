package com.vortix.app.data.battery

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.vortix.app.domain.model.AppUsageItem
import com.vortix.app.domain.model.DormantAppItem
import com.vortix.app.domain.repository.UsageStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidUsageStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageStatsRepository {

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun loadTodayUsage(): List<AppUsageItem> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext emptyList()
        val usageManager = context.getSystemService(UsageStatsManager::class.java)
            ?: return@withContext emptyList()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val launcherApps = runCatching {
            context.packageManager.queryIntentActivities(
                android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(
                    android.content.Intent.CATEGORY_LAUNCHER
                ),
                PackageManager.MATCH_ALL
            ).associateBy { it.activityInfo.packageName }
        }.getOrDefault(emptyMap())

        usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            .orEmpty()
            .asSequence()
            .filter { it.totalTimeInForeground > 0L && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .map { (packageName, records) ->
                val foreground = records.sumOf { it.totalTimeInForeground }
                val lastUsed = records.maxOfOrNull { it.lastTimeUsed } ?: 0L
                val activityInfo = launcherApps[packageName]?.activityInfo
                val label = activityInfo?.applicationInfo?.let { info ->
                    runCatching { context.packageManager.getApplicationLabel(info).toString() }
                        .getOrNull()
                }?.takeIf { it.isNotBlank() } ?: packageName
                AppUsageItem(packageName, label, foreground, lastUsed)
            }
            .sortedByDescending { it.foregroundTimeMillis }
            .take(8)
            .toList()
    }

    override suspend fun loadDormantApps(minimumUnusedDays: Int): List<DormantAppItem> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext emptyList()
        val usageManager = context.getSystemService(UsageStatsManager::class.java)
            ?: return@withContext emptyList()
        val now = System.currentTimeMillis()
        val days = minimumUnusedDays.coerceIn(7, 180)
        val lookback = 90L * 24L * 60L * 60L * 1000L
        val usageByPackage = runCatching {
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - lookback, now)
                .orEmpty().groupBy { it.packageName }
        }.getOrElse { emptyMap() }
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val launchableApps = runCatching {
            context.packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())
        launchableApps.asSequence()
            .mapNotNull { resolveInfo ->
                val info = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (info.packageName == context.packageName) return@mapNotNull null
                val installed = runCatching { context.packageManager.getPackageInfo(info.packageName, 0).firstInstallTime }
                    .getOrDefault(0L)
                val lastUsed = usageByPackage[info.packageName]
                    ?.maxOfOrNull { it.lastTimeUsed }
                    ?.takeIf { it > 0L }
                // Missing usage records are not proof that an app has never been used.
                val daysUnused = lastUsed?.let { ((now - it).coerceAtLeast(0L) / 86_400_000L) }
                val label = runCatching { context.packageManager.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
                DormantAppItem(
                    packageName = info.packageName,
                    displayName = label,
                    lastUsedEpochMillis = lastUsed,
                    firstInstalledEpochMillis = installed,
                    daysSinceLastUse = daysUnused,
                    observedAsDormant = daysUnused != null && daysUnused >= days
                )
            }
            .filter { it.observedAsDormant }
            .sortedByDescending { it.daysSinceLastUse ?: 0L }
            .take(50)
            .toList()
    }

}
