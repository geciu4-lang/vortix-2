package com.vortix.app.feature.insights

import com.vortix.app.domain.model.BatterySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BatteryHistoryCsv {
    fun build(samples: List<BatterySnapshot>): String {
        val output = StringBuilder()
        output.appendLine(
            "timestamp_utc,level_percent,status,plugged_source,temperature_c,voltage_mv,charge_counter_uah,current_ua,cycle_count,health,screen_interactive,thermal_status,power_save_mode,device_idle_mode,platform_discharge_prediction_ms"
        )
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        samples.sortedBy { it.capturedAtEpochMillis }.forEach { sample ->
            val values = listOf(
                dateFormat.format(Date(sample.capturedAtEpochMillis)),
                sample.levelPercent?.toString().orEmpty(),
                sample.status.name,
                sample.pluggedSource.name,
                sample.temperatureCelsius?.toString().orEmpty(),
                sample.voltageMillivolts?.toString().orEmpty(),
                sample.chargeCounterMicroAh?.toString().orEmpty(),
                sample.currentNowMicroAmps?.toString().orEmpty(),
                sample.cycleCount?.toString().orEmpty(),
                sample.health?.name.orEmpty(),
                sample.screenInteractive?.toString().orEmpty(),
                sample.thermalStatus?.toString().orEmpty(),
                sample.powerSaveMode?.toString().orEmpty(),
                sample.deviceIdleMode?.toString().orEmpty(),
                sample.dischargePredictionMillis?.toString().orEmpty()
            )
            output.appendLine(values.joinToString(",") { escape(it) })
        }
        return output.toString()
    }

    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
