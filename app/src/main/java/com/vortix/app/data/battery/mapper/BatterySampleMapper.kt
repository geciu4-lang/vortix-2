package com.vortix.app.data.battery.mapper

import com.vortix.app.data.local.entity.BatterySampleEntity
import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource

fun BatterySnapshot.toEntity(): BatterySampleEntity = BatterySampleEntity(
    capturedAtEpochMillis = capturedAtEpochMillis,
    levelPercent = levelPercent,
    statusName = status.name,
    pluggedSourceName = pluggedSource.name,
    temperatureCelsius = temperatureCelsius,
    voltageMillivolts = voltageMillivolts,
    chargeCounterMicroAh = chargeCounterMicroAh,
    currentNowMicroAmps = currentNowMicroAmps,
    cycleCount = cycleCount,
    healthName = health?.name,
    screenInteractive = screenInteractive,
    thermalStatus = thermalStatus,
    powerSaveMode = powerSaveMode,
    deviceIdleMode = deviceIdleMode,
    dischargePredictionMillis = dischargePredictionMillis
)

fun BatterySampleEntity.toDomain(): BatterySnapshot = BatterySnapshot(
    capturedAtEpochMillis = capturedAtEpochMillis,
    levelPercent = levelPercent,
    status = enumValueOrDefault<BatteryStatus>(statusName, BatteryStatus.UNKNOWN),
    pluggedSource = enumValueOrDefault<PluggedSource>(pluggedSourceName, PluggedSource.UNKNOWN),
    temperatureCelsius = temperatureCelsius,
    voltageMillivolts = voltageMillivolts,
    chargeCounterMicroAh = chargeCounterMicroAh,
    currentNowMicroAmps = currentNowMicroAmps,
    cycleCount = cycleCount,
    health = healthName?.let { enumValueOrDefault<BatteryHealth>(it, BatteryHealth.UNKNOWN) },
    screenInteractive = screenInteractive,
    thermalStatus = thermalStatus,
    powerSaveMode = powerSaveMode,
    deviceIdleMode = deviceIdleMode,
    dischargePredictionMillis = dischargePredictionMillis
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
