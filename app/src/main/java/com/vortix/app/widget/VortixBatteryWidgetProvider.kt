package com.vortix.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.widget.RemoteViews
import com.vortix.app.MainActivity
import com.vortix.app.R
import java.util.Locale

class VortixBatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, manager, appWidgetIds)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VortixBatteryWidgetProvider::class.java))
            if (ids.isNotEmpty()) update(context, manager, ids)
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val intent = if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
            val level = intent?.let {
                val value = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (value >= 0 && scale > 0) (value * 100f / scale).toInt() else null
            }
            val temperature = intent?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10.0)
            val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Şarj oluyor"
                BatteryManager.BATTERY_STATUS_FULL -> "Dolu"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Deşarj"
                else -> "Durum bilinmiyor"
            }
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.vortix_battery_widget)
                views.setTextViewText(R.id.widget_level, level?.let { "$it%" } ?: "—")
                views.setTextViewText(R.id.widget_status, status)
                views.setTextViewText(
                    R.id.widget_temperature,
                    temperature?.let { String.format(Locale.getDefault(), "%.1f°C", it) } ?: "Sıcaklık —"
                )
                val openIntent = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openIntent)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
