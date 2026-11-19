package com.vortix.app.domain.model

data class AppUsageItem(
    val packageName: String,
    val displayName: String,
    val foregroundTimeMillis: Long,
    val lastTimeUsedMillis: Long
)
