package com.vortix.app.domain.model

/** Confidence describes evidence quality, not a guarantee that a battery measurement is exact. */
enum class EvidenceConfidence { INSUFFICIENT, LEARNING, LOW, MEDIUM, HIGH }

data class BatteryCapacityEstimate(
    val estimatedFullCapacityMicroAh: Long?,
    val designCapacityMicroAh: Long?,
    val healthPercent: Int?,
    val acceptedIntervals: Int,
    val confidence: EvidenceConfidence,
    val relativeSpreadPercent: Double?
)

data class ChargeSessionInsight(
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val startLevelPercent: Int?,
    val endLevelPercent: Int?,
    val peakTemperatureCelsius: Double?,
    val averageChargeCurrentMilliAmps: Double?,
    val source: PluggedSource,
    /** Positive net fuel-gauge charge-counter delta; null when unsupported or implausible. */
    val addedChargeMilliAmpHours: Double? = null
) {
    val durationMillis: Long get() = (endedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)
}

data class DrainRateInsight(
    val screenOnPercentPerHour: Double?,
    val screenOffPercentPerHour: Double?,
    val screenOnIntervals: Int,
    val screenOffIntervals: Int,
    val estimatedRuntimeMinutesAtCurrentLevel: Long?,
    val latestUnpluggedDurationMillis: Long?
)

data class DischargeSessionInsight(
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val startLevelPercent: Int?,
    val endLevelPercent: Int?,
    val observedScreenOnMillis: Long,
    val observedScreenOffMillis: Long
) {
    val durationMillis: Long get() = (endedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)
}

data class ThermalExposureInsight(
    val observedMinutesAtOrAbove40C: Long,
    val qualifyingIntervals: Int,
    val highestObservedCelsius: Double?,
    val coverageNote: String
)

data class ScreenOffDrainAnomaly(
    val recentPercentPerHour: Double,
    val baselinePercentPerHour: Double,
    val increasePercent: Double,
    val recentIntervals: Int,
    val baselineIntervals: Int
)

data class PowerInsightSummary(
    val capacity: BatteryCapacityEstimate,
    val drain: DrainRateInsight,
    val screenOffDrainAnomaly: ScreenOffDrainAnomaly? = null,
    val recentChargeSessions: List<ChargeSessionInsight>,
    val recentDischargeSessions: List<DischargeSessionInsight>,
    val thermalExposure: ThermalExposureInsight,
    val sampleCount: Int,
    val historySpanMillis: Long,
    val lastSampleAtEpochMillis: Long?,
    val samplingCoveragePercent: Int?,
    val longestSamplingGapMillis: Long?
)


data class EnergyChangeMarker(
    val atEpochMillis: Long,
    val title: String
)

data class DrainComparison(
    val beforeScreenOffPercentPerHour: Double?,
    val afterScreenOffPercentPerHour: Double?,
    val changePercent: Double?,
    val beforeIntervals: Int,
    val afterIntervals: Int
)
