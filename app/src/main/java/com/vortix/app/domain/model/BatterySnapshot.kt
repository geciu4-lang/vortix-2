package com.vortix.app.domain.model

data class BatterySnapshot(
    val capturedAtEpochMillis: Long,
    val levelPercent: Int?,
    val status: BatteryStatus,
    val pluggedSource: PluggedSource,
    val temperatureCelsius: Double?,
    val voltageMillivolts: Int?,
    val chargeCounterMicroAh: Long?,
    val currentNowMicroAmps: Long?,
    val cycleCount: Int?,
    val health: BatteryHealth?,
    /** Device interaction state at sample time; null means the platform state was unavailable. */
    val screenInteractive: Boolean? = null,
    /** Android thermal throttling status (API 29+), not a battery temperature measurement. */
    val thermalStatus: Int? = null,
    val powerSaveMode: Boolean? = null,
    val deviceIdleMode: Boolean? = null,
    /** Platform-provided discharge ETA (API 31+); null while charging or unsupported. */
    val dischargePredictionMillis: Long? = null
) {
    /**
     * Approximate battery-side electrical power in milliwatts, derived from Android's
     * signed battery current (µA) and terminal voltage (mV). Positive means charging;
     * negative means discharging. This is not USB adapter input power or PD negotiation data.
     */
    val batteryPowerMilliWatts: Double?
        get() {
            val current = currentNowMicroAmps ?: return null
            val voltage = voltageMillivolts?.takeIf { it > 0 } ?: return null
            return (current.toDouble() * voltage.toDouble() / 1_000_000.0)
                .takeIf { it.isFinite() }
        }
}

enum class BatteryStatus {
    UNKNOWN,
    CHARGING,
    DISCHARGING,
    NOT_CHARGING,
    FULL
}

enum class PluggedSource {
    UNKNOWN,
    NONE,
    AC,
    USB,
    WIRELESS,
    DOCK,
    OTHER
}

enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD
}
