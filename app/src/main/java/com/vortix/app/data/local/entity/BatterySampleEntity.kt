package com.vortix.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_samples",
    indices = [Index(value = ["capturedAtEpochMillis"])]
)
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capturedAtEpochMillis: Long,
    val levelPercent: Int?,
    val statusName: String,
    val pluggedSourceName: String,
    val temperatureCelsius: Double?,
    val voltageMillivolts: Int?,
    val chargeCounterMicroAh: Long?,
    val currentNowMicroAmps: Long?,
    val cycleCount: Int?,
    val healthName: String?,
    val screenInteractive: Boolean? = null,
    val thermalStatus: Int? = null,
    val powerSaveMode: Boolean? = null,
    val deviceIdleMode: Boolean? = null,
    val dischargePredictionMillis: Long? = null
)
