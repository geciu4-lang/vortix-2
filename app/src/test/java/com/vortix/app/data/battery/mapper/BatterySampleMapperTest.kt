package com.vortix.app.data.battery.mapper

import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import org.junit.Assert.assertEquals
import org.junit.Test

class BatterySampleMapperTest {
    @Test
    fun snapshotRoundTripsThroughRoomEntity() {
        val original = BatterySnapshot(
            capturedAtEpochMillis = 1_800_000_000_000,
            levelPercent = 73,
            status = BatteryStatus.CHARGING,
            pluggedSource = PluggedSource.USB,
            temperatureCelsius = 31.7,
            voltageMillivolts = 4_120,
            chargeCounterMicroAh = 2_450_000,
            currentNowMicroAmps = 850_000,
            cycleCount = 142,
            health = BatteryHealth.GOOD
        )

        val restored = original.toEntity().toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun unknownEnumNameFallsBackWithoutCrashing() {
        val entity = BatterySnapshot(
            capturedAtEpochMillis = 1L,
            levelPercent = null,
            status = BatteryStatus.UNKNOWN,
            pluggedSource = PluggedSource.OTHER,
            temperatureCelsius = null,
            voltageMillivolts = null,
            chargeCounterMicroAh = null,
            currentNowMicroAmps = null,
            cycleCount = null,
            health = null
        ).toEntity().copy(statusName = "FUTURE_STATUS")

        assertEquals(BatteryStatus.UNKNOWN, entity.toDomain().status)
        assertEquals(PluggedSource.UNKNOWN, entity.copy(pluggedSourceName = "FUTURE_SOURCE").toDomain().pluggedSource)
    }
}
