package com.vortix.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySnapshotTest {
    @Test
    fun derivesBatterySidePowerWithAndroidCurrentSign() {
        val charging = snapshot(currentMicroAmps = 500_000, voltageMilliVolts = 4_000)
        val discharging = snapshot(currentMicroAmps = -200_000, voltageMilliVolts = 4_000)

        assertEquals(2_000.0, charging.batteryPowerMilliWatts!!, 0.0001)
        assertEquals(-800.0, discharging.batteryPowerMilliWatts!!, 0.0001)
    }

    @Test
    fun doesNotInventPowerWhenCurrentOrVoltageIsMissing() {
        assertNull(snapshot(currentMicroAmps = null, voltageMilliVolts = 4_000).batteryPowerMilliWatts)
        assertNull(snapshot(currentMicroAmps = 500_000, voltageMilliVolts = null).batteryPowerMilliWatts)
        assertNull(snapshot(currentMicroAmps = 500_000, voltageMilliVolts = 0).batteryPowerMilliWatts)
    }

    private fun snapshot(currentMicroAmps: Long?, voltageMilliVolts: Int?) = BatterySnapshot(
        capturedAtEpochMillis = 1L,
        levelPercent = 50,
        status = BatteryStatus.DISCHARGING,
        pluggedSource = PluggedSource.NONE,
        temperatureCelsius = 28.0,
        voltageMillivolts = voltageMilliVolts,
        chargeCounterMicroAh = 2_000_000L,
        currentNowMicroAmps = currentMicroAmps,
        cycleCount = null,
        health = BatteryHealth.GOOD
    )
}
