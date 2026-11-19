package com.vortix.app.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Build
import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

interface BatteryDataSource {
    fun readSnapshot(): BatterySnapshot
}

@Singleton
class AndroidBatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryDataSource {
    override fun readSnapshot(): BatterySnapshot {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // This is a protected system broadcast and we only read its sticky snapshot.
            context.registerReceiver(null, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(null, batteryFilter)
        }

        val intentLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val intentScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelFromIntent = if (intentLevel >= 0 && intentScale > 0) {
            (intentLevel * 100f / intentScale).roundToInt().coerceIn(0, 100)
        } else {
            null
        }
        val levelFromProperty = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

        val chargeCounter = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it >= 0 }
            ?.toLong()

        val currentNow = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Int.MIN_VALUE }
            ?.toLong()

        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            batteryIntent
                ?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                ?.takeIf { it >= 0 }
        } else {
            null
        }

        val temperatureTenths = batteryIntent
            ?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltage = batteryIntent
            ?.takeIf { it.hasExtra(BatteryManager.EXTRA_VOLTAGE) }
            ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        return BatterySnapshot(
            capturedAtEpochMillis = System.currentTimeMillis(),
            levelPercent = levelFromIntent ?: levelFromProperty,
            status = mapStatus(
                batteryIntent?.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            ),
            pluggedSource = batteryIntent
                ?.takeIf { it.hasExtra(BatteryManager.EXTRA_PLUGGED) }
                ?.let { mapPluggedSource(it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) }
                ?: PluggedSource.UNKNOWN,
            temperatureCelsius = temperatureTenths?.div(10.0),
            voltageMillivolts = voltage?.takeIf { it > 0 },
            chargeCounterMicroAh = chargeCounter,
            currentNowMicroAmps = currentNow,
            cycleCount = cycleCount,
            health = batteryIntent?.let {
                mapHealth(
                    it.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN
                    )
                )
            },
            screenInteractive = powerManager?.isInteractive,
            thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager?.currentThermalStatus
            } else null,
            powerSaveMode = powerManager?.isPowerSaveMode,
            deviceIdleMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isDeviceIdleMode
            } else null,
            dischargePredictionMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { powerManager?.batteryDischargePrediction?.toMillis() }.getOrNull()
            } else null
        )
    }

    private fun mapStatus(value: Int): BatteryStatus = when (value) {
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
        BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
        else -> BatteryStatus.UNKNOWN
    }

    private fun mapPluggedSource(value: Int): PluggedSource = when (value) {
        0 -> PluggedSource.NONE
        BatteryManager.BATTERY_PLUGGED_AC -> PluggedSource.AC
        BatteryManager.BATTERY_PLUGGED_USB -> PluggedSource.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedSource.WIRELESS
        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            value == BatteryManager.BATTERY_PLUGGED_DOCK
        ) {
            PluggedSource.DOCK
        } else {
            PluggedSource.OTHER
        }
    }

    private fun mapHealth(value: Int): BatteryHealth = when (value) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }
}
