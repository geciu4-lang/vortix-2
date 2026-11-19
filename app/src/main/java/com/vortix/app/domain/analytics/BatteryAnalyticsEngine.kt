package com.vortix.app.domain.analytics

import com.vortix.app.domain.model.BatteryCapacityEstimate
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.ChargeSessionInsight
import com.vortix.app.domain.model.DrainRateInsight
import com.vortix.app.domain.model.DischargeSessionInsight
import com.vortix.app.domain.model.EvidenceConfidence
import com.vortix.app.domain.model.DrainComparison
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.domain.model.PowerInsightSummary
import com.vortix.app.domain.model.ScreenOffDrainAnomaly
import com.vortix.app.domain.model.ThermalExposureInsight
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Conservative analytics over locally persisted Android battery samples.
 * Derived values are emitted only when intervals are sufficiently long and internally consistent.
 * This class intentionally does not infer per-app mAh consumption from UsageStats.
 */
object BatteryAnalyticsEngine {
    private const val MIN_CAPACITY_DELTA_PERCENT = 15
    private const val MIN_INTERVAL_MILLIS = 10 * 60 * 1000L
    private const val MAX_CAPACITY_INTERVAL_MILLIS = 48 * 60 * 60 * 1000L
    private const val MAX_DRAIN_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L
    private const val MAX_THERMAL_INTERVAL_MILLIS = 45 * 60 * 1000L
    private const val HIGH_TEMP_CELSIUS = 40.0

    fun summarize(
        rawSamples: List<BatterySnapshot>,
        designCapacityMicroAh: Long? = null
    ): PowerInsightSummary {
        val samples = rawSamples.sortedBy { it.capturedAtEpochMillis }
            .distinctBy { it.capturedAtEpochMillis }
        val capacity = estimateCapacity(samples, designCapacityMicroAh)
        val drain = calculateDrain(samples)
        val screenOffAnomaly = calculateScreenOffDrainAnomaly(samples)
        val chargeSessions = findChargeSessions(samples).takeLast(8).asReversed()
        val dischargeSessions = findDischargeSessions(samples).takeLast(14).asReversed()
        val thermal = calculateThermalExposure(samples)
        val firstAt = samples.firstOrNull()?.capturedAtEpochMillis
        val lastAt = samples.lastOrNull()?.capturedAtEpochMillis
        val gaps = samples.zipWithNext { first, second ->
            (second.capturedAtEpochMillis - first.capturedAtEpochMillis).coerceAtLeast(0L)
        }
        val coverage = gaps.takeIf { it.isNotEmpty() }?.let { intervals ->
            (intervals.count { it <= 45L * 60L * 1000L } * 100.0 / intervals.size).roundToInt()
        }

        return PowerInsightSummary(
            capacity = capacity,
            drain = drain,
            screenOffDrainAnomaly = screenOffAnomaly,
            recentChargeSessions = chargeSessions,
            recentDischargeSessions = dischargeSessions,
            thermalExposure = thermal,
            sampleCount = samples.size,
            historySpanMillis = if (firstAt != null && lastAt != null) (lastAt - firstAt).coerceAtLeast(0) else 0L,
            lastSampleAtEpochMillis = lastAt,
            samplingCoveragePercent = coverage,
            longestSamplingGapMillis = gaps.maxOrNull()
        )
    }

    fun compareScreenOffDrainAround(
        rawSamples: List<BatterySnapshot>,
        markerAtEpochMillis: Long,
        windowHours: Long = 48L
    ): DrainComparison {
        val samples = rawSamples.sortedBy { it.capturedAtEpochMillis }
        val window = windowHours.coerceIn(6L, 168L) * 60L * 60L * 1000L
        fun rates(from: Long, until: Long): List<Double> {
            val windowSamples = samples.filter { it.capturedAtEpochMillis in from..until }
            val values = ArrayList<Double>()
            for (index in 1 until windowSamples.size) {
                val first = windowSamples[index - 1]
                val second = windowSamples[index]
                val elapsed = second.capturedAtEpochMillis - first.capturedAtEpochMillis
                if (elapsed !in MIN_INTERVAL_MILLIS..MAX_DRAIN_INTERVAL_MILLIS) continue
                if (first.screenInteractive != false || second.screenInteractive != false) continue
                if (first.status != BatteryStatus.DISCHARGING || second.status != BatteryStatus.DISCHARGING) continue
                if (first.pluggedSource != PluggedSource.NONE || second.pluggedSource != PluggedSource.NONE) continue
                val delta = (first.levelPercent ?: continue) - (second.levelPercent ?: continue)
                if (delta <= 0 || delta > 25) continue
                values += delta.toDouble() / (elapsed.toDouble() / 3_600_000.0)
            }
            return values
        }
        val before = rates(markerAtEpochMillis - window, markerAtEpochMillis)
        val after = rates(markerAtEpochMillis, markerAtEpochMillis + window)
        val beforeRate = medianOrNull(before)
        val afterRate = medianOrNull(after)
        val change = if (beforeRate != null && beforeRate > 0.0 && afterRate != null) {
            ((afterRate - beforeRate) / beforeRate) * 100.0
        } else null
        return DrainComparison(beforeRate, afterRate, change, before.size, after.size)
    }

    private fun estimateCapacity(
        samples: List<BatterySnapshot>,
        designCapacityMicroAh: Long?
    ): BatteryCapacityEstimate {
        val estimates = ArrayList<Double>()
        var index = 0
        while (index < samples.lastIndex) {
            val start = samples[index]
            val startLevel = start.levelPercent
            val startCounter = start.chargeCounterMicroAh
            if (startLevel == null || startCounter == null) {
                index++
                continue
            }

            var match: BatterySnapshot? = null
            var matchIndex = -1
            for (candidateIndex in (index + 1)..samples.lastIndex) {
                val candidate = samples[candidateIndex]
                val elapsed = candidate.capturedAtEpochMillis - start.capturedAtEpochMillis
                if (elapsed > MAX_CAPACITY_INTERVAL_MILLIS) break
                if (elapsed < MIN_INTERVAL_MILLIS) continue
                val level = candidate.levelPercent ?: continue
                val counter = candidate.chargeCounterMicroAh ?: continue
                if (abs(level - startLevel) < MIN_CAPACITY_DELTA_PERCENT) continue
                val levelDelta = level - startLevel
                val counterDelta = counter - startCounter
                // Fuel-gauge counter and displayed state of charge should move in the same direction.
                if (levelDelta * counterDelta <= 0L) continue
                match = candidate
                matchIndex = candidateIndex
                break
            }

            if (match != null) {
                val endLevel = match.levelPercent
                val endCounter = match.chargeCounterMicroAh
                if (endLevel != null && endCounter != null) {
                    val levelDelta = abs(endLevel - startLevel)
                    val counterDelta = abs(endCounter - startCounter)
                    val fullCapacity = counterDelta.toDouble() * 100.0 / levelDelta.toDouble()
                    // Conservative plausibility guard for phone/tablet batteries; never substitute a guess.
                    if (fullCapacity in 500_000.0..30_000_000.0) estimates += fullCapacity
                }
                index = maxOf(index + 1, matchIndex)
            } else {
                index++
            }
        }

        if (estimates.isEmpty()) {
            return BatteryCapacityEstimate(
                estimatedFullCapacityMicroAh = null,
                designCapacityMicroAh = designCapacityMicroAh?.takeIf { it > 0 },
                healthPercent = null,
                acceptedIntervals = 0,
                confidence = if (samples.size < 2) EvidenceConfidence.INSUFFICIENT else EvidenceConfidence.LEARNING,
                relativeSpreadPercent = null
            )
        }

        val median = median(estimates)
        val deviations = estimates.map { abs(it - median) }
        val medianDeviation = median(deviations)
        val accepted = estimates.filter { median == 0.0 || abs(it - median) <= median * 0.25 }
        val finalMedian = median(accepted.ifEmpty { estimates })
        val spread = if (finalMedian > 0.0) medianDeviation / finalMedian * 100.0 else null
        val observedSpan = (samples.lastOrNull()?.capturedAtEpochMillis ?: 0L) -
            (samples.firstOrNull()?.capturedAtEpochMillis ?: 0L)
        val confidence = when {
            accepted.size >= 16 && observedSpan >= 7L * 24L * 60L * 60L * 1000L && (spread ?: 100.0) <= 7.0 -> EvidenceConfidence.HIGH
            accepted.size >= 8 && observedSpan >= 72L * 60L * 60L * 1000L && (spread ?: 100.0) <= 15.0 -> EvidenceConfidence.MEDIUM
            accepted.size >= 3 && (spread ?: 100.0) <= 30.0 -> EvidenceConfidence.LOW
            else -> EvidenceConfidence.LEARNING
        }
        val design = designCapacityMicroAh?.takeIf { it > 0 }
        // Do not publish a health percentage until evidence reaches at least MEDIUM confidence.
        // The reported health scale is capped at 100%; the raw estimated mAh remains visible so
        // a nominal design-capacity mismatch cannot produce a confusing 105%-150% "health" score.
        val health = if (design != null && confidence in setOf(EvidenceConfidence.MEDIUM, EvidenceConfidence.HIGH)) {
            (finalMedian / design * 100.0).roundToInt().coerceIn(1, 100)
        } else null

        return BatteryCapacityEstimate(
            estimatedFullCapacityMicroAh = finalMedian.roundToInt().toLong(),
            designCapacityMicroAh = design,
            healthPercent = health,
            acceptedIntervals = accepted.size,
            confidence = confidence,
            relativeSpreadPercent = spread
        )
    }

    /**
     * Compares the latest 24-hour screen-off drain with the preceding 7-day personal baseline.
     * Requires multiple real intervals on both sides and both a relative and absolute increase;
     * this avoids turning sparse/noisy data into an anomaly alert.
     */
    private fun calculateScreenOffDrainAnomaly(samples: List<BatterySnapshot>): ScreenOffDrainAnomaly? {
        val latestAt = samples.lastOrNull()?.capturedAtEpochMillis ?: return null
        val dayMillis = 24L * 60L * 60L * 1000L
        val recentStart = latestAt - dayMillis
        val baselineStart = recentStart - 7L * dayMillis
        val baselineRates = ArrayList<Double>()
        val recentRates = ArrayList<Double>()

        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val elapsed = current.capturedAtEpochMillis - previous.capturedAtEpochMillis
            if (elapsed !in MIN_INTERVAL_MILLIS..MAX_DRAIN_INTERVAL_MILLIS) continue
            if (previous.status != BatteryStatus.DISCHARGING || current.status != BatteryStatus.DISCHARGING) continue
            if (previous.pluggedSource != PluggedSource.NONE || current.pluggedSource != PluggedSource.NONE) continue
            if (previous.screenInteractive != false || current.screenInteractive != false) continue
            val startLevel = previous.levelPercent ?: continue
            val endLevel = current.levelPercent ?: continue
            val lost = startLevel - endLevel
            if (lost <= 0 || lost > 25) continue
            val rate = lost.toDouble() / (elapsed.toDouble() / 3_600_000.0)
            when (current.capturedAtEpochMillis) {
                in (baselineStart + 1)..recentStart -> baselineRates += rate
                in (recentStart + 1)..latestAt -> recentRates += rate
            }
        }

        if (baselineRates.size < 5 || recentRates.size < 3) return null
        val baseline = median(baselineRates)
        val recent = median(recentRates)
        if (baseline <= 0.0 || recent - baseline < 1.0 || recent < baseline * 1.75) return null
        return ScreenOffDrainAnomaly(
            recentPercentPerHour = recent,
            baselinePercentPerHour = baseline,
            increasePercent = (recent - baseline) / baseline * 100.0,
            recentIntervals = recentRates.size,
            baselineIntervals = baselineRates.size
        )
    }

    private fun calculateDrain(samples: List<BatterySnapshot>): DrainRateInsight {
        val screenOnRates = ArrayList<Double>()
        val screenOffRates = ArrayList<Double>()

        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val duration = current.capturedAtEpochMillis - previous.capturedAtEpochMillis
            if (duration !in MIN_INTERVAL_MILLIS..MAX_DRAIN_INTERVAL_MILLIS) continue
            if (previous.status != BatteryStatus.DISCHARGING || current.status != BatteryStatus.DISCHARGING) continue
            if (previous.pluggedSource != PluggedSource.NONE || current.pluggedSource != PluggedSource.NONE) continue
            val startLevel = previous.levelPercent ?: continue
            val endLevel = current.levelPercent ?: continue
            val lost = startLevel - endLevel
            if (lost <= 0 || lost > 25) continue
            val rate = lost.toDouble() / (duration.toDouble() / 3_600_000.0)
            when {
                previous.screenInteractive == false && current.screenInteractive == false -> screenOffRates += rate
                previous.screenInteractive == true && current.screenInteractive == true -> screenOnRates += rate
            }
        }

        // This is an observed discharge window, not necessarily the time since the last full charge:
        // the app may have started sampling in the middle of an existing battery cycle.
        var dischargeStart: BatterySnapshot? = null
        var latestDuration: Long? = null
        var previousDischarge: BatterySnapshot? = null
        for (sample in samples) {
            val discharging = sample.status == BatteryStatus.DISCHARGING && sample.pluggedSource == PluggedSource.NONE
            if (!discharging) {
                val start = dischargeStart
                val end = previousDischarge
                if (start != null && end != null) {
                    val elapsed = end.capturedAtEpochMillis - start.capturedAtEpochMillis
                    val startLevel = start.levelPercent
                    val endLevel = end.levelPercent
                    if (elapsed >= 30 * 60 * 1000L && startLevel != null && endLevel != null && startLevel - endLevel >= 5) {
                        latestDuration = elapsed
                    }
                }
                dischargeStart = null
                previousDischarge = null
                continue
            }
            val previous = previousDischarge
            if (previous != null && sample.capturedAtEpochMillis - previous.capturedAtEpochMillis > MAX_DRAIN_INTERVAL_MILLIS) {
                dischargeStart = sample
            } else if (dischargeStart == null) {
                dischargeStart = sample
            }
            previousDischarge = sample
        }
        val openStart = dischargeStart
        val openEnd = previousDischarge
        if (openStart != null && openEnd != null) {
            val elapsed = openEnd.capturedAtEpochMillis - openStart.capturedAtEpochMillis
            val startLevel = openStart.levelPercent
            val endLevel = openEnd.levelPercent
            if (elapsed >= 30 * 60 * 1000L && startLevel != null && endLevel != null && startLevel - endLevel >= 5) {
                latestDuration = elapsed
            }
        }

        val currentLevel = samples.lastOrNull()?.levelPercent
        val runtimeRate = median(screenOnRates.takeLast(12))
        val runtime = if (currentLevel != null && runtimeRate > 0.1 && screenOnRates.size >= 3) {
            (currentLevel / runtimeRate * 60.0).roundToInt().toLong().coerceAtLeast(0L)
        } else null

        return DrainRateInsight(
            screenOnPercentPerHour = medianOrNull(screenOnRates),
            screenOffPercentPerHour = medianOrNull(screenOffRates),
            screenOnIntervals = screenOnRates.size,
            screenOffIntervals = screenOffRates.size,
            estimatedRuntimeMinutesAtCurrentLevel = runtime,
            latestUnpluggedDurationMillis = latestDuration
        )
    }

    private fun findChargeSessions(samples: List<BatterySnapshot>): List<ChargeSessionInsight> {
        val result = ArrayList<ChargeSessionInsight>()
        var active = ArrayList<BatterySnapshot>()
        fun finish() {
            if (active.size >= 2) {
                val first = active.first()
                val last = active.last()
                val currents = active.mapNotNull { it.currentNowMicroAmps }
                    .filter { it > 0 }
                result += ChargeSessionInsight(
                    startedAtEpochMillis = first.capturedAtEpochMillis,
                    endedAtEpochMillis = last.capturedAtEpochMillis,
                    startLevelPercent = first.levelPercent,
                    endLevelPercent = last.levelPercent,
                    peakTemperatureCelsius = active.mapNotNull { it.temperatureCelsius }.maxOrNull(),
                    averageChargeCurrentMilliAmps = currents.takeIf { it.isNotEmpty() }
                        ?.average()?.div(1000.0),
                    source = active.map { it.pluggedSource }.firstOrNull { it != PluggedSource.UNKNOWN }
                        ?: PluggedSource.UNKNOWN,
                    addedChargeMilliAmpHours = run {
                        val firstCounter = first.chargeCounterMicroAh
                        val lastCounter = last.chargeCounterMicroAh
                        val delta = if (firstCounter != null && lastCounter != null) lastCounter - firstCounter else null
                        delta?.takeIf { it in 1L..30_000_000L }?.div(1_000.0)
                    }
                )
            }
            active = ArrayList()
        }

        samples.forEach { sample ->
            val charging = sample.status == BatteryStatus.CHARGING || sample.status == BatteryStatus.FULL
            val connected = sample.pluggedSource !in setOf(PluggedSource.NONE, PluggedSource.UNKNOWN)
            if (charging && connected) {
                val previous = active.lastOrNull()
                if (previous != null && sample.capturedAtEpochMillis - previous.capturedAtEpochMillis > 90 * 60 * 1000L) {
                    finish()
                }
                active += sample
            } else {
                finish()
            }
        }
        finish()
        return result
    }

    private fun findDischargeSessions(samples: List<BatterySnapshot>): List<DischargeSessionInsight> {
        val result = ArrayList<DischargeSessionInsight>()
        var active = ArrayList<BatterySnapshot>()
        fun finish() {
            if (active.size >= 2) {
                val first = active.first()
                val last = active.last()
                var screenOnMillis = 0L
                var screenOffMillis = 0L
                active.zipWithNext().forEach { (before, after) ->
                    val elapsed = after.capturedAtEpochMillis - before.capturedAtEpochMillis
                    if (elapsed !in 0L..MAX_DRAIN_INTERVAL_MILLIS) return@forEach
                    if (before.screenInteractive == true && after.screenInteractive == true) screenOnMillis += elapsed
                    if (before.screenInteractive == false && after.screenInteractive == false) screenOffMillis += elapsed
                }
                result += DischargeSessionInsight(
                    startedAtEpochMillis = first.capturedAtEpochMillis,
                    endedAtEpochMillis = last.capturedAtEpochMillis,
                    startLevelPercent = first.levelPercent,
                    endLevelPercent = last.levelPercent,
                    observedScreenOnMillis = screenOnMillis,
                    observedScreenOffMillis = screenOffMillis
                )
            }
            active = ArrayList()
        }
        samples.forEach { sample ->
            val discharging = sample.status == BatteryStatus.DISCHARGING && sample.pluggedSource == PluggedSource.NONE
            if (!discharging) {
                finish()
            } else {
                val previous = active.lastOrNull()
                if (previous != null && sample.capturedAtEpochMillis - previous.capturedAtEpochMillis > MAX_DRAIN_INTERVAL_MILLIS) {
                    finish()
                }
                active += sample
            }
        }
        finish()
        return result
    }

    private fun calculateThermalExposure(samples: List<BatterySnapshot>): ThermalExposureInsight {
        var millis = 0L
        var intervals = 0
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val elapsed = current.capturedAtEpochMillis - previous.capturedAtEpochMillis
            if (elapsed !in MIN_INTERVAL_MILLIS..MAX_THERMAL_INTERVAL_MILLIS) continue
            val first = previous.temperatureCelsius ?: continue
            val second = current.temperatureCelsius ?: continue
            if (first >= HIGH_TEMP_CELSIUS && second >= HIGH_TEMP_CELSIUS) {
                millis += elapsed
                intervals++
            }
        }
        return ThermalExposureInsight(
            observedMinutesAtOrAbove40C = millis / 60_000L,
            qualifyingIntervals = intervals,
            highestObservedCelsius = samples.mapNotNull { it.temperatureCelsius }.maxOrNull(),
            coverageNote = "Yalnızca aralarında en fazla 45 dakika bulunan kayıtlar sayılır; bu süre sensörün sürekli ölçüm yaptığı anlamına gelmez."
        )
    }

    private fun medianOrNull(values: List<Double>): Double? = values.takeIf { it.isNotEmpty() }?.let(::median)

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0 else sorted[middle]
    }
}
