package com.vortix.app.domain.analytics

import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.EvidenceConfidence
import com.vortix.app.domain.model.PluggedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAnalyticsEngineTest {
    @Test
    fun estimatesCapacityFromRepeatedLongCounterIntervals() {
        val samples = listOf(
            sample(0, 40, 1_600_000, charging = true),
            sample(1, 60, 2_400_000, charging = true),
            sample(2, 80, 3_200_000, charging = true),
            sample(3, 60, 2_400_000, charging = false),
            sample(4, 40, 1_600_000, charging = false)
        )
        val estimate = BatteryAnalyticsEngine.summarize(samples, 4_000_000).capacity

        assertNotNull(estimate.estimatedFullCapacityMicroAh)
        assertEquals(4_000_000L, estimate.estimatedFullCapacityMicroAh)
        assertNull(estimate.healthPercent)
        assertEquals(EvidenceConfidence.LOW, estimate.confidence)
    }

    @Test
    fun refusesCapacityEstimateWhenOnlySmallLevelChangesAreAvailable() {
        val estimate = BatteryAnalyticsEngine.summarize(
            listOf(
                sample(0, 50, 2_000_000, charging = false),
                sample(1, 55, 2_200_000, charging = false)
            ), 4_000_000
        ).capacity

        assertNull(estimate.estimatedFullCapacityMicroAh)
        assertNull(estimate.healthPercent)
        assertEquals(EvidenceConfidence.LEARNING, estimate.confidence)
    }


    @Test
    fun publishesCappedHealthOnlyAfterMediumConfidenceEvidence() {
        val levels = listOf(40, 60, 80, 60, 40, 60, 80, 60, 40, 60, 80, 60, 40, 60, 80)
        val samples = levels.mapIndexed { index, level ->
            BatterySnapshot(
                capturedAtEpochMillis = index * 6L * 60L * 60L * 1000L,
                levelPercent = level,
                status = BatteryStatus.DISCHARGING,
                pluggedSource = PluggedSource.NONE,
                temperatureCelsius = 28.0,
                voltageMillivolts = 4_000,
                chargeCounterMicroAh = level * 45_000L,
                currentNowMicroAmps = -40_000,
                cycleCount = null,
                health = BatteryHealth.GOOD,
                screenInteractive = false
            )
        }
        val estimate = BatteryAnalyticsEngine.summarize(samples, 4_000_000L).capacity

        assertEquals(EvidenceConfidence.MEDIUM, estimate.confidence)
        assertEquals(4_500_000L, estimate.estimatedFullCapacityMicroAh)
        assertEquals(100, estimate.healthPercent)
    }

    @Test
    fun doesNotReportPersonalScreenOffAnomalyWithoutSevenDayBaselineEvidence() {
        val samples = (0..6).map { hour ->
            sample(
                hour = hour,
                level = 90 - hour,
                chargeCounter = (90 - hour) * 40_000L,
                charging = false,
                screenInteractive = false
            )
        }
        assertNull(BatteryAnalyticsEngine.summarize(samples).screenOffDrainAnomaly)
    }

    @Test
    fun detectsRecentScreenOffDrainRiseAgainstSevenDayPersonalBaseline() {
        val samples = buildList {
            for (day in 0..7) {
                val dayStart = day * 24L * 60L * 60L * 1000L
                val recentDay = day == 7
                add(
                    BatterySnapshot(
                        capturedAtEpochMillis = dayStart,
                        levelPercent = 100,
                        status = BatteryStatus.CHARGING,
                        pluggedSource = PluggedSource.AC,
                        temperatureCelsius = 28.0,
                        voltageMillivolts = 4_000,
                        chargeCounterMicroAh = 4_000_000,
                        currentNowMicroAmps = 500_000,
                        cycleCount = null,
                        health = BatteryHealth.GOOD,
                        screenInteractive = false
                    )
                )
                for (hour in 1..23) {
                    val rate = if (recentDay) 3 else 1
                    add(
                        BatterySnapshot(
                            capturedAtEpochMillis = dayStart + hour * 60L * 60L * 1000L,
                            levelPercent = 100 - rate * hour,
                            status = BatteryStatus.DISCHARGING,
                            pluggedSource = PluggedSource.NONE,
                            temperatureCelsius = 28.0,
                            voltageMillivolts = 4_000,
                            chargeCounterMicroAh = (100 - rate * hour) * 40_000L,
                            currentNowMicroAmps = -40_000,
                            cycleCount = null,
                            health = BatteryHealth.GOOD,
                            screenInteractive = false
                        )
                    )
                }
            }
        }
        val anomaly = BatteryAnalyticsEngine.summarize(samples).screenOffDrainAnomaly
        assertNotNull(anomaly)
        assertEquals(1.0, anomaly!!.baselinePercentPerHour, 0.0001)
        assertEquals(3.0, anomaly.recentPercentPerHour, 0.0001)
        assertEquals(200.0, anomaly.increasePercent, 0.0001)
    }

    @Test
    fun calculatesSeparateScreenOffDrainRateOnlyFromObservedIntervals() {
        val samples = listOf(
            sample(0, 80, 3_200_000, charging = false, screenInteractive = false),
            sample(1, 79, 3_160_000, charging = false, screenInteractive = false),
            sample(2, 78, 3_120_000, charging = false, screenInteractive = false),
            sample(3, 77, 3_080_000, charging = false, screenInteractive = false)
        )
        val drain = BatteryAnalyticsEngine.summarize(samples).drain

        assertEquals(1.0, drain.screenOffPercentPerHour!!, 0.0001)
        assertEquals(3, drain.screenOffIntervals)
        assertNull(drain.screenOnPercentPerHour)
    }

    @Test
    fun comparesScreenOffDrainBeforeAndAfterUserChangeMarker() {
        val samples = listOf(
            sample(45, 90, 3_600_000, charging = false),
            sample(46, 89, 3_560_000, charging = false),
            sample(47, 88, 3_520_000, charging = false),
            sample(49, 86, 3_440_000, charging = false),
            sample(50, 84, 3_360_000, charging = false),
            sample(51, 82, 3_280_000, charging = false)
        )
        val comparison = BatteryAnalyticsEngine.compareScreenOffDrainAround(
            samples, markerAtEpochMillis = 48L * 60L * 60L * 1000L
        )
        assertEquals(1.0, comparison.beforeScreenOffPercentPerHour!!, 0.0001)
        assertEquals(2.0, comparison.afterScreenOffPercentPerHour!!, 0.0001)
        assertEquals(100.0, comparison.changePercent!!, 0.0001)
    }

    @Test
    fun recordsNetChargeCounterIncreaseForChargeSession() {
        val session = BatteryAnalyticsEngine.summarize(
            listOf(
                sample(0, 40, 1_600_000, charging = true),
                sample(1, 60, 2_400_000, charging = true)
            )
        ).recentChargeSessions.single()

        assertEquals(800.0, session.addedChargeMilliAmpHours!!, 0.001)
    }

    @Test
    fun doesNotInventChargeAddedWhenFuelGaugeCounterFallsDuringSession() {
        val session = BatteryAnalyticsEngine.summarize(
            listOf(
                sample(0, 60, 2_400_000, charging = true),
                sample(1, 65, 2_200_000, charging = true)
            )
        ).recentChargeSessions.single()

        assertNull(session.addedChargeMilliAmpHours)
    }

    @Test
    fun recordsObservedDischargeSessionsWithScreenStateCoverage() {
        val samples = listOf(
            sample(0, 90, 3_600_000, charging = false, screenInteractive = true),
            sample(1, 88, 3_520_000, charging = false, screenInteractive = false),
            sample(2, 86, 3_440_000, charging = false, screenInteractive = false),
            sample(3, 95, 3_800_000, charging = true, screenInteractive = false)
        )
        val session = BatteryAnalyticsEngine.summarize(samples).recentDischargeSessions.single()
        assertEquals(2 * 60L * 60L * 1000L, session.durationMillis)
        assertEquals(1 * 60L * 60L * 1000L, session.observedScreenOnMillis)
        assertEquals(1 * 60L * 60L * 1000L, session.observedScreenOffMillis)
    }

    @Test
    fun reportsSamplingCoverageAndLongestGapWithoutFillingMissingSamples() {
        val samples = listOf(
            sample(0, 90, 3_600_000, charging = false),
            sampleAt(30L * 60L * 1000L, 89, 3_560_000),
            sample(4, 86, 3_440_000, charging = false)
        )
        val summary = BatteryAnalyticsEngine.summarize(samples)
        assertEquals(50, summary.samplingCoveragePercent)
        assertEquals(3L * 60L * 60L * 1000L + 30L * 60L * 1000L, summary.longestSamplingGapMillis)
        assertEquals(3, summary.sampleCount)
    }

    @Test
    fun capacityConfidenceRequiresRepeatedIntervals() {
        val samples = listOf(
            sample(0, 40, 1_600_000, charging = true),
            sample(1, 60, 2_400_000, charging = true),
            sample(2, 80, 3_200_000, charging = true)
        )
        val estimate = BatteryAnalyticsEngine.summarize(samples).capacity
        assertEquals(EvidenceConfidence.LEARNING, estimate.confidence)
        assertEquals(2, estimate.acceptedIntervals)
    }

    private fun sampleAt(
        timestamp: Long,
        level: Int,
        chargeCounter: Long
    ) = BatterySnapshot(
        capturedAtEpochMillis = timestamp,
        levelPercent = level,
        status = BatteryStatus.DISCHARGING,
        pluggedSource = PluggedSource.NONE,
        temperatureCelsius = 28.0,
        voltageMillivolts = 4_000,
        chargeCounterMicroAh = chargeCounter,
        currentNowMicroAmps = -40_000,
        cycleCount = null,
        health = BatteryHealth.GOOD,
        screenInteractive = false
    )

    private fun sample(
        hour: Int,
        level: Int,
        chargeCounter: Long,
        charging: Boolean,
        screenInteractive: Boolean? = false
    ) = BatterySnapshot(
        capturedAtEpochMillis = hour * 60L * 60L * 1000L,
        levelPercent = level,
        status = if (charging) BatteryStatus.CHARGING else BatteryStatus.DISCHARGING,
        pluggedSource = if (charging) PluggedSource.AC else PluggedSource.NONE,
        temperatureCelsius = 28.0,
        voltageMillivolts = 4_000,
        chargeCounterMicroAh = chargeCounter,
        currentNowMicroAmps = if (charging) 500_000 else -200_000,
        cycleCount = 100,
        health = BatteryHealth.GOOD,
        screenInteractive = screenInteractive
    )
}
