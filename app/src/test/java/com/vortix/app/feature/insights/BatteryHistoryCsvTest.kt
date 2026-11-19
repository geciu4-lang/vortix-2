package com.vortix.app.feature.insights

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHistoryCsvTest {
    @Test
    fun exportContainsRawBatteryFieldsAndNoInventedValues() {
        val csv = BatteryHistoryCsv.build(
            listOf(
                BatterySnapshot(
                    capturedAtEpochMillis = 0L,
                    levelPercent = 73,
                    status = BatteryStatus.DISCHARGING,
                    pluggedSource = PluggedSource.NONE,
                    temperatureCelsius = null,
                    voltageMillivolts = 3_900,
                    chargeCounterMicroAh = null,
                    currentNowMicroAmps = -200_000,
                    cycleCount = null,
                    health = null
                )
            )
        )
        assertTrue(csv.contains("level_percent"))
        assertTrue(csv.contains(",73,DISCHARGING,NONE,"))
        assertTrue(csv.contains(",,3900,, -200000".replace(" ", "")))
    }
}
