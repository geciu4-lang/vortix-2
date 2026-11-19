package com.vortix.app.domain.analytics

import com.vortix.app.domain.model.BatteryCapacityEstimate
import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.DrainRateInsight
import com.vortix.app.domain.model.EvidenceConfidence
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.domain.model.PowerInsightSummary
import com.vortix.app.domain.model.RecommendationPriority
import com.vortix.app.domain.model.ScreenOffDrainAnomaly
import com.vortix.app.domain.model.ThermalExposureInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerAdvisorEngineTest {
    @Test
    fun criticalBatteryTemperatureCreatesCriticalRecommendation() {
        val result = PowerAdvisorEngine.evaluate(
            snapshot(temperature = 46.0),
            emptySummary()
        )
        assertEquals("critical_battery_temperature", result.first().id)
        assertEquals(RecommendationPriority.CRITICAL, result.first().priority)
    }

    @Test
    fun lowBatteryWithPowerSaveOffSuggestsNativePowerSaver() {
        val result = PowerAdvisorEngine.evaluate(
            snapshot(level = 14, powerSaveMode = false),
            emptySummary()
        )
        assertTrue(result.any { it.id == "enable_power_saver" })
    }

    @Test
    fun absoluteScreenOffDrainWarningRequiresThreeIntervalsAndVeryHighRate() {
        val drain = DrainRateInsight(
            screenOnPercentPerHour = 1.0,
            screenOffPercentPerHour = 3.2,
            screenOnIntervals = 4,
            screenOffIntervals = 2,
            estimatedRuntimeMinutesAtCurrentLevel = null,
            latestUnpluggedDurationMillis = null
        )
        val noWarning = PowerAdvisorEngine.evaluate(snapshot(), emptySummary(drain.copy(screenOffIntervals = 3)))
        assertFalse(noWarning.any { it.id == "elevated_screen_off_drain" })

        val enoughEvidence = PowerAdvisorEngine.evaluate(
            snapshot(), emptySummary(drain.copy(screenOffPercentPerHour = 5.2, screenOffIntervals = 3))
        )
        assertTrue(enoughEvidence.any { it.id == "elevated_screen_off_drain" })
    }

    @Test
    fun personalDrainAnomalyGetsAnEvidenceBasedRecommendation() {
        val summary = emptySummary().copy(
            screenOffDrainAnomaly = ScreenOffDrainAnomaly(
                recentPercentPerHour = 3.2,
                baselinePercentPerHour = 1.4,
                increasePercent = 128.6,
                recentIntervals = 5,
                baselineIntervals = 18
            )
        )
        val result = PowerAdvisorEngine.evaluate(snapshot(), summary)
        assertTrue(result.any { it.id == "personal_screen_off_drain_anomaly" })
    }

    @Test
    fun learningCapacityNeverProducesBatteryFailureDiagnosis() {
        val summary = emptySummary().copy(
            capacity = BatteryCapacityEstimate(
                estimatedFullCapacityMicroAh = 2_000_000,
                designCapacityMicroAh = null,
                healthPercent = null,
                acceptedIntervals = 1,
                confidence = EvidenceConfidence.LEARNING,
                relativeSpreadPercent = null
            )
        )
        val result = PowerAdvisorEngine.evaluate(snapshot(), summary)
        assertTrue(result.any { it.id == "capacity_learning" })
        assertFalse(result.any { it.id == "low_estimated_capacity" })
    }

    private fun snapshot(
        level: Int = 70,
        temperature: Double = 30.0,
        powerSaveMode: Boolean? = true
    ) = BatterySnapshot(
        capturedAtEpochMillis = 1_000L,
        levelPercent = level,
        status = BatteryStatus.DISCHARGING,
        pluggedSource = PluggedSource.NONE,
        temperatureCelsius = temperature,
        voltageMillivolts = 4_000,
        chargeCounterMicroAh = 2_000_000,
        currentNowMicroAmps = -200_000,
        cycleCount = null,
        health = BatteryHealth.GOOD,
        screenInteractive = false,
        thermalStatus = 0,
        powerSaveMode = powerSaveMode,
        deviceIdleMode = false,
        dischargePredictionMillis = null
    )

    private fun emptySummary(drain: DrainRateInsight = DrainRateInsight(null, null, 0, 0, null, null)) =
        PowerInsightSummary(
            capacity = BatteryCapacityEstimate(null, null, null, 0, EvidenceConfidence.INSUFFICIENT, null),
            drain = drain,
            recentChargeSessions = emptyList(),
            recentDischargeSessions = emptyList(),
            thermalExposure = ThermalExposureInsight(0, 0, null, ""),
            sampleCount = 0,
            historySpanMillis = 0L,
            lastSampleAtEpochMillis = null,
            samplingCoveragePercent = null,
            longestSamplingGapMillis = null
        )
}
