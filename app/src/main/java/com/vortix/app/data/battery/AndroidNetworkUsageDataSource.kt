package com.vortix.app.data.battery

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Process
import com.vortix.app.domain.model.NetworkTrafficBucket
import com.vortix.app.domain.model.NetworkUsageSnapshot
import com.vortix.app.domain.repository.NetworkUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidNetworkUsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkUsageRepository {

    override suspend fun loadLast24Hours(): NetworkUsageSnapshot = withContext(Dispatchers.IO) {
        val end = System.currentTimeMillis()
        val start = end - 24L * 60L * 60L * 1000L
        val appOps = context.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val access = appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
        if (!access) return@withContext NetworkUsageSnapshot(
            start, end, null, null, false,
            "Wi-Fi ve mobil veri istatistikleri için Android Kullanım erişimi gerekir."
        )

        val manager = context.getSystemService(NetworkStatsManager::class.java)
            ?: return@withContext NetworkUsageSnapshot(
                start, end, null, null, true, "Bu cihaz NetworkStatsManager hizmetini sunmuyor."
            )

        // Summary is device-wide for the requested interval. It is deliberately kept separate
        // from detailed history: queryDetails is profile-scoped and returns native time buckets.
        fun readSummary(type: Int): Long? = runCatching {
            val bucket = manager.querySummaryForDevice(type, null, start, end)
            safeByteSum(bucket.rxBytes, bucket.txBytes)
        }.getOrNull()

        fun readHistory(type: Int): List<NetworkTrafficBucket>? = runCatching {
            val stats = manager.queryDetails(type, null, start, end) ?: return@runCatching null
            try {
                val totals = linkedMapOf<Pair<Long, Long>, Long>()
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    if (!stats.getNextBucket(bucket)) break
                    val from = bucket.startTimeStamp
                    val until = bucket.endTimeStamp
                    if (until <= from) continue
                    val bytes = safeByteSum(bucket.rxBytes, bucket.txBytes)
                    if (bytes > 0L) {
                        val key = from to until
                        totals[key] = safeByteSum(totals[key] ?: 0L, bytes)
                    }
                }
                totals.map { (range, bytes) ->
                    NetworkTrafficBucket(range.first, range.second, bytes)
                }.sortedBy { it.startEpochMillis }
            } finally {
                stats.close()
            }
        }.getOrNull()

        val mobile = readSummary(ConnectivityManager.TYPE_MOBILE)
        val wifi = readSummary(ConnectivityManager.TYPE_WIFI)
        val mobileHistory = readHistory(ConnectivityManager.TYPE_MOBILE)
        val wifiHistory = readHistory(ConnectivityManager.TYPE_WIFI)
        NetworkUsageSnapshot(
            startEpochMillis = start,
            endEpochMillis = end,
            mobileBytes = mobile,
            wifiBytes = wifi,
            hasUsageAccess = true,
            note = "Toplamlar cihaz genelindeki ağ trafiğidir; geçmiş çubukları Android'in saat ölçeğindeki doğal zaman kovalarıdır ve kullanıcı profiline aittir. Ağ trafiği pil tüketimi veya mAh ölçümü değildir. Üretici veri sunmazsa ilgili alan boş kalır.",
            mobileHistoryBuckets = mobileHistory,
            wifiHistoryBuckets = wifiHistory
        )
    }

    private fun safeByteSum(first: Long, second: Long): Long {
        val a = first.coerceAtLeast(0L)
        val b = second.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b
    }
}
