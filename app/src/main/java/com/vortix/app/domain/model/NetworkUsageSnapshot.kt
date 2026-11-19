package com.vortix.app.domain.model

data class NetworkTrafficBucket(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val bytes: Long
)

data class NetworkUsageSnapshot(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val mobileBytes: Long?,
    val wifiBytes: Long?,
    val hasUsageAccess: Boolean,
    val note: String,
    /** Native Android history buckets; null means unavailable, empty means no recorded buckets. */
    val mobileHistoryBuckets: List<NetworkTrafficBucket>? = null,
    val wifiHistoryBuckets: List<NetworkTrafficBucket>? = null
)

data class DormantAppItem(
    val packageName: String,
    val displayName: String,
    val lastUsedEpochMillis: Long?,
    val firstInstalledEpochMillis: Long,
    val daysSinceLastUse: Long?,
    val observedAsDormant: Boolean
)
